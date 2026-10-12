#!/bin/bash
set -euo pipefail

source "buildScript/init/env.sh"
ENV_NB4A=1
source "buildScript/lib/core/get_source_env.sh"

PROJECT_ROOT="$(pwd)"
PATCHES_DIR="$(realpath "$PROJECT_ROOT/patches")"

# Clone (or update), force-checkout the target commit (full tree), then apply patch(es).
# patch_file can be a single file, a directory of *.patch, or empty.
fetch_and_patch() {
  local name="$1"
  local repo="$2"
  local commit="$3"
  local patch_spec="${4:-}"   # file, directory, or empty
  local dir_name="${5:-$name}"

  if [ ! -d "$dir_name/.git" ]; then
    echo "==> Cloning $name from $repo"
    rm -rf "$dir_name"
    git clone "$repo" "$dir_name"
  fi

  pushd "$dir_name" > /dev/null

  if ! git cat-file -e "${commit}^{commit}" 2>/dev/null; then
    echo "==> Fetching $name ($commit)"
    git fetch --depth 1 origin "$commit" 2>/dev/null || \
      git fetch --depth 1 origin tag "$commit" 2>/dev/null || \
      git fetch origin
  fi

  echo "==> Checking out $name @ $commit (full tree)"
  git checkout --force "$commit"
  git reset --hard "$commit"
  git clean -fdx -e .git 2>/dev/null || true

  # Collect patch files
  local patches=()
  if [ -n "$patch_spec" ]; then
    if [ -d "$patch_spec" ]; then
      while IFS= read -r -d '' f; do
        patches+=("$f")
      done < <(find "$patch_spec" -maxdepth 1 -type f -name "*.patch" | sort -z)
    elif [ -f "$patch_spec" ]; then
      patches+=("$patch_spec")
    fi
  fi

  for patch_file in "${patches[@]:-}"; do
    [ -f "$patch_file" ] || continue
    echo "==> Applying patch for $name: $(basename "$patch_file")"
    if git apply --check "$patch_file" 2>/dev/null; then
      git apply "$patch_file"
      echo "    applied cleanly with git apply"
    elif patch -p1 --dry-run < "$patch_file" >/dev/null 2>&1; then
      patch -p1 < "$patch_file"
      echo "    applied cleanly with patch -p1"
    elif git apply --3way "$patch_file" 2>/dev/null; then
      echo "    applied with git apply --3way"
    else
      echo "ERROR: Failed to apply patch for $name: $patch_file"
      git status --short | head -20
      exit 1
    fi
  done

  popd > /dev/null
}

echo "==> Fetching and patching dependencies (patches from $PATCHES_DIR)"
pushd ..

#### sing-box (must be first — nested patches for amneziawg-go / utls live inside it)
fetch_and_patch "sing-box" \
  "$REPO_SING_BOX" \
  "$COMMIT_SING_BOX" \
  "$PATCHES_DIR/sing-box.patch"

SING_BOX_DIR="$(pwd)/sing-box"

#### amneziawg-go (patches come from sing-box/patches/amneziawg-go after sing-box is patched)
fetch_and_patch "amneziawg-go" \
  "$REPO_AMNEZIAWG_GO" \
  "$COMMIT_AMNEZIAWG_GO" \
  "$SING_BOX_DIR/patches/amneziawg-go"

#### utls (patches come from sing-box/patches/utls)
fetch_and_patch "utls" \
  "$REPO_UTLS" \
  "$COMMIT_UTLS" \
  "$SING_BOX_DIR/patches/utls"

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

#### MasterDnsVPN (expected as MasterDnsVPN-plus by core.docker.sh / go.mod)
fetch_and_patch "MasterDnsVPN" \
  "$REPO_MASTERDNSVPN" \
  "$COMMIT_MASTERDNSVPN" \
  "$PATCHES_DIR/MasterDnsVPN.patch" \
  "MasterDnsVPN-plus"

popd
echo "==> All dependencies fetched and patched."
