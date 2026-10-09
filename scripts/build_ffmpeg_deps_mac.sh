# git clone https://github.com/xiph/ogg.git
# git clone https://github.com/xiph/vorbis.git
# git clone https://github.com/breakfastquay/rubberband.git
root=$PWD
cat > rb_x86_64.txt <<'EOF'
[binaries]
c = ['cc', '-arch', 'x86_64']
cpp = ['c++', '-arch', 'x86_64', '-include', 'cstddef']
ar = ['ar']
strip = ['strip']
pkg-config = 'pkg-config'

[host_machine]
system = 'darwin'
cpu_family = 'x86_64'
cpu = 'x86_64'
endian = 'little'
EOF
cat > rb_arm64.txt <<'EOF'
[binaries]
c = ['cc', '-arch', 'arm64']
cpp = ['c++', '-arch', 'arm64', '-include', 'cstddef']
ar = ['ar']
strip = ['strip']
pkg-config = 'pkg-config'

[host_machine]
system = 'darwin'
cpu_family = 'aarch64'
cpu = 'arm64'
endian = 'little'
EOF
for arch in x86_64 arm64; do
  cross="--cross-file $root/rb_$arch.txt"
  prefix="$root/build_mac_deps/$arch"
  mkdir -p "$prefix"
  cmake -S ogg -B "build/$arch/ogg" \
    -DCMAKE_OSX_ARCHITECTURES=$arch \
    -DCMAKE_INSTALL_PREFIX="$prefix" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF
  cmake --build "build/$arch/ogg" -j6 --target install
  cmake -S vorbis -B "build/$arch/vorbis" \
    -DCMAKE_OSX_ARCHITECTURES=$arch \
    -DCMAKE_INSTALL_PREFIX="$prefix" \
    -DCMAKE_PREFIX_PATH="$prefix" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF
  cmake --build "build/$arch/vorbis" -j6 --target install
  meson setup "rubberband/build_$arch" rubberband $cross \
    --prefix "$prefix" \
    --libdir lib \
    -Ddefault_library=static \
    -Dcmdline=disabled \
    -Dtests=disabled
  meson compile -C "rubberband/build_$arch"
  mkdir -p "$prefix/lib/pkgconfig" "$prefix/include/rubberband"
  cp "rubberband/build_$arch/librubberband.a" "$prefix/lib/"
  cp "rubberband/build_$arch/meson-private/rubberband.pc" "$prefix/lib/pkgconfig/"
  cp rubberband/rubberband/rubberband-c.h "$prefix/include/rubberband/"
done
mkdir -p "$root/build_mac_deps/lib/pkgconfig" "$root/build_mac_deps/include"
for lib in libogg libvorbis libvorbisenc libvorbisfile librubberband; do
  lipo -create \
    "$root/build_mac_deps/x86_64/lib/$lib.a" \
    "$root/build_mac_deps/arm64/lib/$lib.a" \
    -output "$root/build_mac_deps/lib/$lib.a"
done
cp -R "$root/build_mac_deps/arm64/include/." "$root/build_mac_deps/include/"
for pc in "$root/build_mac_deps/x86_64/lib/pkgconfig/"*.pc; do
  sed -e "s|^prefix=.*|prefix=$root/build_mac_deps|" \
      -e "s|^libdir=.*|libdir=$root/build_mac_deps/lib|" \
      -e "s|^includedir=.*|includedir=$root/build_mac_deps/include|" \
    "$pc" > "$root/build_mac_deps/lib/pkgconfig/$(basename $pc)"
done
hash -r