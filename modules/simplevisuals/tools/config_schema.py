"""Read retained config fields directly from Java for bilingual producer validation."""
from pathlib import Path
import re
MODULE = Path(__file__).resolve().parents[1]
PREFIX = {'SimplevisualsConfig': '', 'Visuals': 'visuals.', 'SpeedLines': 'visuals.speedLines.',
          'PickupNotifier': 'visuals.pickupNotifier.', 'DamageIndicators': 'visuals.damageIndicators.',
          'BiomeInfo': 'visuals.biomeInfo.', 'HeldItemTooltips': 'visuals.heldItemTooltips.', 'Particles': 'particles.'}
TAB = {'Visuals': 'general', 'SpeedLines': 'speed', 'PickupNotifier': 'pickup',
       'DamageIndicators': 'damage', 'BiomeInfo': 'biome', 'HeldItemTooltips': 'tooltips', 'Particles': 'particles'}
def options():
    text=(MODULE/'shared/java/com/simplevisuals/config/SimplevisualsConfig.java').read_text(encoding='utf-8')
    classes=[]
    for match in re.finditer(r'class\s+(\w+)\s*\{',text):
        start=match.end(); depth=1; end=start
        while depth:
            depth += (text[end]=='{')-(text[end]=='}');end+=1
        classes.append((match.group(1),start,end))
    out=[]
    pattern=r'public\s+(boolean|int|float|PickupSide|PickupLayout|DeathCoordsMode|com\.simplevisuals\.effects\.Intensity)\s+(\w+)\s*=\s*([^;]+);'
    for field in re.finditer(pattern,text):
        owners=[c for c in classes if c[1]<=field.start()<c[2]]
        cls=min(owners,key=lambda c:c[2]-c[1])[0]
        typ,name,default=field.groups();default=default.strip().removesuffix('f')
        if typ not in ('boolean','int','float'):default=default.rsplit('.',1)[-1]
        if '0x' in default:default=str(int(default,16))
        out.append(dict(path=PREFIX[cls]+name,type=typ,default=default,tab=TAB[cls]))
    return out
