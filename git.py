#!/usr/bin/env python3
"""Link current git repo to personal or work GitHub account."""

import subprocess
import sys
from pathlib import Path

ACCOUNTS = {
    "personal": {
        "username": "prafullKrRj",
        "email": "prafullkumar384@gmail.com",
        "ssh_host": "github-personal",
    },
    "work": {
        "username": "prafull-solytics",
        "email": "prafull.kumar@solytics-partners.com",
        "ssh_host": "github-work",
    },
}


def run(cmd, **kwargs):
    return subprocess.run(cmd, check=True, text=True, capture_output=True, **kwargs)


def set_git_config(account):
    run(["git", "config", "user.name", account["username"]])
    run(["git", "config", "user.email", account["email"]])
    print(f"  user.name  = {account['username']}")
    print(f"  user.email = {account['email']}")


def fix_remote(account, remote="origin"):
    try:
        result = run(["git", "remote", "get-url", remote])
        url = result.stdout.strip()
    except subprocess.CalledProcessError:
        print(f"  No remote '{remote}' found, skipping URL update.")
        return

    import re
    # HTTPS: https://github.com/user/repo.git → git@host:user/repo.git
    https_match = re.match(r"https://github\.com/(.+)", url)
    if https_match:
        new_url = f"git@{account['ssh_host']}:{https_match.group(1)}"
    else:
        # SSH: swap host alias only
        new_url = re.sub(r"git@[^:]+:", f"git@{account['ssh_host']}:", url)

    if new_url != url:
        run(["git", "remote", "set-url", remote, new_url])
        print(f"  remote url = {new_url}")
    else:
        print(f"  remote url unchanged: {url}")


def verify_ssh(account):
    result = subprocess.run(
        ["ssh", "-T", f"git@{account['ssh_host']}"],
        text=True, capture_output=True
    )
    ok = "successfully authenticated" in result.stderr
    print(f"  SSH {'OK' if ok else 'FAIL'}: {result.stderr.strip()}")
    return ok


def link(name):
    if not Path(".git").exists():
        print("Not a git repo.")
        sys.exit(1)

    account = ACCOUNTS[name]
    print(f"\nLinking repo to [{name}] account...")
    set_git_config(account)
    fix_remote(account)
    verify_ssh(account)
    print(f"\nDone. Repo configured for [{name}].")


def pick_interactively():
    print("\nAccounts:")
    for i, name in enumerate(ACCOUNTS, 1):
        a = ACCOUNTS[name]
        print(f"  {i}. {name:10}  {a['username']} <{a['email']}>")
    choice = input("\nPick (1/2) or name: ").strip()
    names = list(ACCOUNTS)
    if choice == "1":
        return names[0]
    elif choice == "2":
        return names[1]
    elif choice in ACCOUNTS:
        return choice
    else:
        print("Invalid choice.")
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] in ACCOUNTS:
        link(sys.argv[1])
    else:
        if len(sys.argv) > 1:
            print(f"Unknown account '{sys.argv[1]}'. Use: personal | work")
            sys.exit(1)
        link(pick_interactively())