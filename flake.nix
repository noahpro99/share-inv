{
  description = "Minecraft Fabric mod development environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs =
    {
      nixpkgs,
      flake-utils,
      ...
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            openjdk
            gradle
            git
          ];
          shellHook = ''
            echo "Welcome to the Minecraft Fabric mod dev shell!"
            echo "Java: $(java -version 2>&1 | head -n 1)"
            echo "Gradle: $(gradle --version | head -n 1)"
            echo "You may want to run 'gradle genSources' or 'gradle build'."
          '';
        };
      }
    );
}
