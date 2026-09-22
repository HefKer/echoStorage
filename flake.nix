{
  description = "Fabric mod dev environment - Minecraft 1.21.1";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

      # MC 1.21.1 declares javaVersion.majorVersion = 21 in Mojang's manifest.
      jdk = pkgs.jdk21;

      # LWJGL and Gradle both extract unpatched prebuilt .so files at runtime and
      # dlopen their dependencies by bare soname, so they need these on the path.
      runtimeLibs = with pkgs; [
        stdenv.cc.cc.lib

        # windowing
        glfw3-minecraft
        libGL
        libx11
        libxcursor
        libxext
        libxrandr
        libxxf86vm
        libxi
        wayland
        libdecor
        libxkbcommon

        # audio: LWJGL's bundled libopenal.so dlopens these backends directly
        openal
        alsa-lib
        libjack2
        libpulseaudio
        pipewire

        udev # oshi hardware detection at startup

        # Gradle's own extracted JNI natives (libnative-platform-curses.so)
        ncurses
        zlib
      ];
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          jdk
          pkgs.jdt-language-server
          pkgs.google-java-format
          pkgs.pciutils # lspci, probed at client startup
        ];

        JAVA_HOME = "${jdk}/lib/openjdk";

        # Driver link goes first, matching nixpkgs' own prismlauncher wrapper.
        LD_LIBRARY_PATH =
          "${pkgs.addDriverRunpath.driverLink}/lib:" + pkgs.lib.makeLibraryPath runtimeLibs;
      };
    };
}
