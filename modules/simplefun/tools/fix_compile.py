from pathlib import Path
M=Path(__file__).resolve().parents[1]
def r(p,a,b):p=M/p;s=p.read_text();assert a in s,(p,a);p.write_text(s.replace(a,b))
r('shared/java/com/simplefun/mixin/NoDamageMixin.java','victim.knockback(Math.min(2.4,strength),Math.sin(angle),-Math.cos(angle));','victim.knockback(Math.min(2.4,strength),Math.sin(angle),-Math.cos(angle),source,0);')
r('shared/java/com/simplefun/test/FunTests.java','h.getBlockEntity(new BlockPos(2,2,2))','h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(2,2,2)))')
r('shared/java/com/simplefun/test/FunTests.java','pig.invulnerableTime=0;','pig.setInvulnerableTime(0);')
r('shared/java/com/simplefun/test/FunTests.java','s.getItemHolder().unwrapKey().orElseThrow().identifier()','BuiltInRegistries.ITEM.getKey(s.getItem())')
