# git clone https://git.ffmpeg.org/ffmpeg.git ffmpeg
# cd ffmpeg
for arch in x86_64 arm64; do
  case $arch in
    x86_64) build=build_mac_x64 ;;
    *) build=build_mac_arm64 ;;
  esac
  make clean
  PATH="$build/bin:$PATH" PKG_CONFIG_PATH="../build_mac_deps/lib/pkgconfig" ./configure \
    --prefix="$build/ffmpeg_build" \
    --arch=$arch --target-os=darwin \
    --enable-cross-compile \
    --pkg-config-flags="--static" \
    --extra-cflags="-arch $arch -I../build_mac_deps/include" \
    --extra-ldflags="-arch $arch -L../build_mac_deps/lib" \
    --extra-libs="-lpthread -lm" \
    --bindir="$build/bin" \
      --enable-pic \
      --enable-static \
      --disable-shared \
      --disable-doc \
      --disable-debug \
      --disable-avdevice \
      --disable-swscale \
      --disable-programs \
      --enable-ffmpeg \
      --disable-network \
      --disable-muxers \
      --disable-demuxers \
      --disable-zlib \
      --disable-lzma \
      --disable-bzlib \
      --disable-iconv \
      --disable-libxcb \
      --disable-bsfs \
      --disable-filters \
      --disable-parsers \
      --disable-indevs \
      --disable-outdevs \
      --disable-encoders \
      --disable-decoders \
      --disable-hwaccels \
      --disable-vdpau \
      --disable-vulkan \
      --disable-vaapi \
      --disable-nvdec \
      --disable-nvenc \
      --disable-libdrm \
      --disable-ffnvcodec \
      --disable-dxva2 \
      --disable-d3d12va \
      --disable-d3d11va \
      --disable-cuvid \
      --disable-cuda-llvm \
      --disable-amf \
      --disable-videotoolbox \
      --disable-audiotoolbox \
      --disable-filters \
      --disable-protocols \
      --enable-protocol=file \
      --enable-libvorbis \
      --enable-librubberband \
      --enable-gpl \
      --enable-decoder=pcm_s16le \
      --enable-demuxer=wav \
      --enable-encoder=aac \
      --enable-encoder=flac \
      --enable-encoder=libvorbis \
      --enable-encoder=pcm_s16le \
      --enable-muxer=ogg \
      --enable-muxer=flac \
      --enable-muxer=adts \
      --enable-muxer=wav \
      --enable-filter=aresample \
      --enable-filter=rubberband \
      --enable-filter=atempo
  PATH="$build/bin:$PATH" make -j6 && \
  make install && \
  hash -r
done
lipo -create \
  build_mac_x64/bin/ffmpeg \
  build_mac_arm64/bin/ffmpeg \
  -output ffmpeg_macOS_universal
hash -r
