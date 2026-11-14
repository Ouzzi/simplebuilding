#!/usr/bin/env python3
"""usage: build_preview.py <area.json> <out.html> - injects the area JSON into preview_template.html."""
import sys
from pathlib import Path
t = Path(__file__).with_name("preview_template.html").read_text()
Path(sys.argv[2]).write_text(t.replace("/*DATA*/null", Path(sys.argv[1]).read_text()))
