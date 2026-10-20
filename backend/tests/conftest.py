import sys
from pathlib import Path

# Add backend and root directory to sys.path so 'import app...' and 'import worker...' work anywhere
backend_dir = Path(__file__).resolve().parent.parent
root_dir = backend_dir.parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))
if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))
