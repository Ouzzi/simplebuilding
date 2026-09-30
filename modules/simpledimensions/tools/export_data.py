"""Export registry/balance producer data from the module's implementation."""
import json,re
from pathlib import Path
M=Path(__file__).resolve().parents[1]
R=M/'shared/java/com/simplebuilding/modules/simpledimensions'
registry=(R/'DimensionRegistry.java').read_text()
ids=re.findall(r'=block\("([^" ]+)"\)',registry)
assert set(ids)=={'sky_portal','light_blue_portal'}
wiki=M/'generated/resources/wiki';wiki.mkdir(parents=True,exist_ok=True)
(wiki/'items.json').write_text(json.dumps({'items':[{'id':'simpledimension:'+id,'kind':'block'} for id in ids]},indent=2)+'\n')
constants={name:float(value) if '.' in value else int(value) for name,value in re.findall(r'public static final (?:int|double) ([A-Z_]+)\s*=\s*([.0-9]+);',(R/'ConfigLimits.java').read_text())}
balance=M.parents[1]/'balance/simpledimensions';balance.mkdir(parents=True,exist_ok=True)
(balance/'limits.json').write_text(json.dumps({'source':'modules/simpledimensions/shared/java/com/simplebuilding/modules/simpledimensions/ConfigLimits.java','limits':constants},indent=2)+'\n')
print('simpledimensions: exported registry inventory and safety caps')
