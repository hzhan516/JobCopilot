#!/usr/bin/env python3
"""日志管理脚本，用于代码审查和修复循环。"""
import sys
import os
from datetime import datetime
from pathlib import Path


def init(title: str, log_file: str):
    """初始化日志文件。"""
    path = Path(log_file)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(f"# {title}\n\n")
        f.write(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
    print(str(path.resolve()))


def write(iteration: int, scope: str, fixes: str, risk: str, has_risk: str, log_file: str):
    """追加一次迭代记录到日志文件。"""
    path = Path(log_file)
    status = "CONTINUE" if has_risk.lower() in ("true", "yes", "1") else "COMPLETE"
    with open(path, "a", encoding="utf-8") as f:
        f.write(f"\n## Iteration {iteration} | {scope} | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} | Status: {status}\n\n")
        f.write(f"### Fixes Applied\n{fixes}\n\n")
        f.write(f"### Risk Assessment\n{risk}\n\n")
        if status == "CONTINUE":
            f.write("### Decision\nRisk remains. Proceed to next review cycle.\n")
        else:
            f.write("### Decision\nNo remaining risk. Cycle complete.\n")
    print(f"Logged iteration {iteration} to {path}")


def finalize(log_file: str, iterations: int):
    """追加最终状态到日志文件。"""
    path = Path(log_file)
    with open(path, "a", encoding="utf-8") as f:
        f.write(f"\n---\nCycle complete after {iterations} iterations.\n")
    print(f"Finalized log at {path}")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "init" and len(sys.argv) >= 4:
        init(sys.argv[2], sys.argv[3])
    elif cmd == "write" and len(sys.argv) >= 7:
        write(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7] if len(sys.argv) > 7 else "fix-log.md")
    elif cmd == "finalize" and len(sys.argv) >= 4:
        finalize(sys.argv[2], int(sys.argv[3]))
    else:
        print("Usage: log_manager.py init <title> <log_file>")
        print("       log_manager.py write <iteration> <scope> <fixes> <risk> <has_risk> [log_file]")
        print("       log_manager.py finalize <log_file> <iterations>")
        sys.exit(1)
