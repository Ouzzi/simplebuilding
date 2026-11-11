"""Unit tests for the MIXCHECK31-3 additions of check_client_mixins.py (synthetic members, no jars)."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_client_mixins as c

REDIRECT, MODARGS = c.INJ + "Redirect;", c.INJ + "ModifyArgs;"


def member(name, desc, insns=()):
    return c.Member(0, name, desc, {}, set(insns))


def field_redirect(handler_desc, field_desc="I"):
    target = member("tick", "()V", [("FIELD", "net/mc/Foo", "count", field_desc)])
    ann = {"at": ("x", {"value": "FIELD", "target": f"Lnet/mc/Foo;count:{field_desc}"})}
    problems = []
    c.check_signature("M", member("h", handler_desc), REDIRECT, "@Redirect", [target], ann, problems)
    return problems


class FieldRedirect(unittest.TestCase):
    def test_getter_and_setter_shapes_pass(self):
        for desc in ("(Lnet/mc/Foo;)I", "()I", "(Lnet/mc/Foo;I)V", "(I)V"):
            self.assertEqual(field_redirect(desc), [], desc)

    def test_wrong_type_or_arity_fails(self):
        for desc in ("(Lnet/mc/Foo;)Z", "(Lnet/mc/Foo;Z)V", "(Lnet/mc/Foo;II)V", "()V"):
            self.assertEqual(len(field_redirect(desc)), 1, desc)

    def test_object_field_compared_loosely(self):
        self.assertEqual(field_redirect("(Lnet/mc/Foo;)Ljava/lang/Object;", "Ljava/util/List;"), [])


class ModifyArgs(unittest.TestCase):
    def check(self, desc):
        problems = []
        c.check_modify_args("M", member("h", desc), "@ModifyArgs", problems)
        return problems

    def test_args_handler(self):
        self.assertEqual(self.check("(Lorg/spongepowered/asm/mixin/injection/invoke/arg/Args;)V"), [])
        self.assertEqual(self.check("(Lorg/spongepowered/asm/mixin/injection/invoke/arg/Args;I)V"), [])

    def test_bad_handlers(self):
        self.assertEqual(len(self.check("(I)V")), 1)
        self.assertEqual(len(self.check("()V")), 1)
        self.assertEqual(len(self.check("(Lorg/spongepowered/asm/mixin/injection/invoke/arg/Args;)I")), 1)


class Plugin(unittest.TestCase):
    PLUGIN = "org/spongepowered/asm/mixin/extensibility/IMixinConfigPlugin"

    class FakeCp:
        def __init__(self, classes):
            self.classes = classes

        def get(self, name):
            return self.classes.get(name)

        def known(self, name):
            return name in self.classes

    @staticmethod
    def info(name, sup="java/lang/Object", interfaces=()):
        ci = c.ClassInfo()
        ci.name, ci.super, ci.interfaces = name, sup, list(interfaces)
        return ci

    def run_check(self, classes, plugin="a.P"):
        problems = []
        c.check_plugin(self.FakeCp(classes), "m.json", {"plugin": plugin}, problems)
        return problems

    def test_ok_direct_and_inherited(self):
        self.assertEqual(self.run_check({"a/P": self.info("a/P", interfaces=[self.PLUGIN])}), [])
        self.assertEqual(self.run_check({"a/P": self.info("a/P", sup="a/Base"),
                                         "a/Base": self.info("a/Base", interfaces=[self.PLUGIN])}), [])

    def test_missing_class_and_wrong_type(self):
        self.assertEqual(len(self.run_check({})), 1)
        self.assertEqual(len(self.run_check({"a/P": self.info("a/P")})), 1)

    def test_unresolved_super_gives_no_verdict(self):
        self.assertEqual(self.run_check({"a/P": self.info("a/P", sup="lib/Unknown")}), [])

    def test_no_plugin_key(self):
        problems = []
        c.check_plugin(self.FakeCp({}), "m.json", {}, problems)
        self.assertEqual(problems, [])


class LocalCapture(unittest.TestCase):
    """b-mixlocal27: @Local against the target's local variable table."""
    WRAP = c.MX + "wrapoperation/WrapOperation;"
    CALL = ("INVOKE", "net/mc/Foo", "draw", "(I)V")

    def target(self, lvt, static=False):
        # instance method tick(Lnet/mc/Level;I)V: this=0, level=1, n=2; locals pos (slot 3, offsets 4..20), a second
        # BlockPos other (slot 4, offsets 12..20); draw() called at offsets 8 and 16; return at 20
        return c.Member(0x0008 if static else 0, "tick", "(Lnet/mc/Level;I)V", {}, {self.CALL},
                        lvt=lvt, pcs={self.CALL: [8, 16], ("RETURN",): [20]})

    LVT = [(0, 21, "this", "Lnet/mc/Foo;", 0), (0, 21, "level", "Lnet/mc/Level;", 1), (0, 21, "n", "I", 2),
           (4, 17, "pos", "Lnet/mc/BlockPos;", 3), (12, 9, "other", "Lnet/mc/BlockPos;", 4)]

    def check(self, local_desc, local_ann, at=None, lvt=LVT, sig=None):
        at = at or {"value": "INVOKE", "target": "Lnet/mc/Foo;draw(I)V"}
        handler = c.Member(0, "h", f"(ILcom/llamalad7/mixinextras/injector/wrapoperation/Operation;{local_desc})V", {}, set(),
                           params=[{}, {}, {c.LOCAL: local_ann}], sig=sig)
        problems = []
        c.check_locals("M", "net/mc/Foo", handler, "@WrapOperation", [self.target(lvt)], {"at": ("x", at)}, problems)
        return problems

    def test_implicit_unique_in_scope(self):
        # at offset 8 only pos is in scope; at 16 pos and other -> ambiguous
        self.assertEqual(self.check("Lnet/mc/Level;", {}), [])
        self.assertEqual(self.check("Lnet/mc/BlockPos;", {}, {"value": "INVOKE", "target": "Lnet/mc/Foo;draw(I)V", "ordinal": ("int", 0)}), [])
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {})), 1)

    def test_missing_type(self):
        self.assertEqual(len(self.check("Lnet/mc/Entity;", {})), 1)

    def test_ordinal_name_index(self):
        second = {"value": "INVOKE", "target": "Lnet/mc/Foo;draw(I)V", "ordinal": ("int", 1)}
        self.assertEqual(self.check("Lnet/mc/BlockPos;", {"ordinal": ("int", 1)}, second), [])
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"ordinal": ("int", 1)})), 1)  # first call: only pos in scope
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"ordinal": ("int", 2)}, second)), 1)
        self.assertEqual(self.check("Lnet/mc/BlockPos;", {"name": ["other"]}, second), [])
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"name": ["other"]})), 1)
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"name": ["missing"]})), 1)
        self.assertEqual(self.check("Lnet/mc/BlockPos;", {"index": ("int", 4)}, second), [])
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"index": ("int", 2)})), 1)

    def test_this_is_not_a_local(self):
        self.assertEqual(len(self.check("Lnet/mc/Foo;", {})), 1)

    def test_args_only(self):
        self.assertEqual(self.check("I", {"argsOnly": ("int", 1)}), [])
        self.assertEqual(self.check("Lnet/mc/Level;", {"argsOnly": ("int", 1)}), [])
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {"argsOnly": ("int", 1)})), 1)
        self.assertEqual(len(self.check("I", {"argsOnly": ("int", 1), "ordinal": ("int", 1)})), 1)

    def test_ref_types(self):
        self.assertEqual(self.check("Lcom/llamalad7/mixinextras/sugar/ref/LocalIntRef;", {}), [])
        self.assertEqual(len(self.check("Lcom/llamalad7/mixinextras/sugar/ref/LocalFloatRef;", {})), 1)
        sig = "(ILcom/llamalad7/mixinextras/injector/wrapoperation/Operation<Ljava/lang/Void;>;Lcom/llamalad7/mixinextras/sugar/ref/LocalRef<Lnet/mc/Level;>;)V"
        self.assertEqual(self.check("Lcom/llamalad7/mixinextras/sugar/ref/LocalRef;", {}, sig=sig), [])
        self.assertEqual(len(self.check("Lcom/llamalad7/mixinextras/sugar/ref/LocalRef;", {}, sig=sig.replace("Level", "Entity"))), 1)

    def test_head_and_unmodelled_at(self):
        self.assertEqual(len(self.check("Lnet/mc/BlockPos;", {}, {"value": "HEAD"})), 1)  # pos not yet in scope
        self.assertEqual(self.check("Lnet/mc/BlockPos;", {}, {"value": "JUMP"}), [])  # whole method: present, no uniqueness

    def test_no_table_no_verdict(self):
        self.assertEqual(self.check("Lnet/mc/Entity;", {}, lvt=None), [])


if __name__ == "__main__":
    unittest.main()
