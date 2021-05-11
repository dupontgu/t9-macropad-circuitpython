import os
from shutil import copyfile, copytree
from pathlib import Path

platform_dirs = set([d for d in os.walk('.')][0][1])
platform_dirs = platform_dirs - {'core', 'bundle'}
# platform_dirs.remove('core')
# platform_dirs.remove('bundle')

for pd in platform_dirs:
    print(pd)
    path = Path(f"./bundle/{pd}")
    path.mkdir(parents=True, exist_ok=True)
    copytree('core', path, dirs_exist_ok=True)
    copytree(pd, path, dirs_exist_ok=True)
