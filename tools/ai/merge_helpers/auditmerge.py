import re,os,sys
f=sys.argv[1]
s=open(f,encoding='utf-8').read()
def res(m):
    h=m.group(1).splitlines(); t=m.group(2).splitlines()
    if len(h)!=len(t):
        return m.group(1)+m.group(2)
    out=[]
    for a,b in zip(h,t):
        p=os.path.commonprefix([a,b])
        out.append(a+b[len(p):] if len(b)>len(p) else a)
    return "\n".join(out)+"\n"
s=re.sub(r'<<<<<<< [^\n]*\n(.*?)=======\n(.*?)>>>>>>> [^\n]*\n',res,s,flags=re.S)
open(f,'w',encoding='utf-8').write(s)
