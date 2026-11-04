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


if __name__ == "__main__":
    unittest.main()
