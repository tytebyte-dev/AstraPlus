#!/bin/bash
set -e

source "buildScript/init/env.sh"
ENV_NB4A=1
source "buildScript/lib/core/get_source_env.sh"

PROJECT_ROOT="$(pwd)"
PATCHES_DIR="$(realpath "$PROJECT_ROOT/patches")"

# Helper: clone (if needed), checkout target commit, apply patch if present
fetch_and_patch() {
  local name="$1"
  local repo="$2"
  local commit="$3"
  local patch_file="$4"   # absolute path or empty
  local dir_name="${5:-$name}"

  if [ ! -d "$dir_name" ]; then
    echo "==> Cloning $name from $repo"
    git clone --no-checkout "$repo" "$dir_name"
  fi

  pushd "$dir_name" > /dev/null

  # Make sure we have the commit
  if ! git cat-file -e "$commit^{commit}" 2>/dev/null; then
    echo "==> Fetching $name"
    git fetch --depth 1 origin "$commit" || git fetch origin
  fi

  current="$(git rev-parse HEAD 2>/dev/null || echo none)"
  if [ "$current" != "$commit" ]; then
    echo "==> Checking out $name @ $commit"
    git checkout --force "$commit"
  else
    echo "==> $name already at $commit"
  fi

  # Apply patch if provided and not already applied
  if [ -n "$patch_file" ] && [ -f "$patch_file" ]; then
    if git apply --check "$patch_file" 2>/dev/null; then
      echo "==> Applying patch for $name: $(basename "$patch_file")"
      git apply "$patch_file"
    else
      # Maybe already applied – try reverse check
      if git apply --reverse --check "$patch_file" 2>/dev/null; then
        echo "==> Patch for $name already applied, skipping"
      else
        echo "==> Warning: could not apply patch for $name cleanly, trying with --3way"
        git apply --3way "$patch_file" || {
          echo "ERROR: Failed to apply patch for $name"
          exit 1
        }
      fi
    fi
  fi

  popd > /dev/null
}

pushd ..

#### sing-box
fetch_and_patch "sing-box" \
  "$REPO_SING_BOX" \
  "$COMMIT_SING_BOX" \
  "$PATCHES_DIR/sing-box.patch"

#### sing-vmess
fetch_and_patch "sing-vmess" \
  "$REPO_SING_VMESS" \
  "$COMMIT_SING_VMESS" \
  "$PATCHES_DIR/sing-vmess.patch"

#### libneko
fetch_and_patch "libneko" \
  "$REPO_LIBNEKO" \
  "$COMMIT_LIBNEKO" \
  "$PATCHES_DIR/libneko.patch"

#### byedpi
fetch_and_patch "byedpi" \
  "$REPO_BYEDPI" \
  "$COMMIT_BYEDPI" \
  "$PATCHES_DIR/byedpi/0001-byedpi-jni-api.patch"

#### MasterDnsVPN (used as MasterDnsVPN-plus by docker/build)
fetch_and_patch "MasterDnsVPN" \
  "$REPO_MASTERDNSVPN" \
  "$COMMIT_MASTERDNSVPN" \
  "$PATCHES_DIR/MasterDnsVPN.patch" \
  "MasterDnsVPN-plus"

popd

echo "==> All dependencies fetched and patched."
