# FelixSkin v1.1.4 - Minecraft Fabric Mod

A client-only Fabric mod for Minecraft 1.20.1 that allows players to drag and drop PNG skin files directly into the game and apply them immediately without restarting.

## ✨ Features

- **🎯 Drag & Drop Interface**: Open GUI with K key, drag PNG files from your OS into the drop zone
- **⚡ Immediate Application**: Skins are applied instantly to your player model
- **🎨 HD Support**: Supports skins of any resolution (128x128, 256x256, 512x512, etc.)
- **🤖 Auto-Detection**: Automatically detects slim (Alex) vs classic (Steve) models based on filename
  - Files containing `_slim` or `_alex` → Alex model
  - All other files → Steve model
- **🌐 Client-Only**: No server-side changes required
- **💾 Memory Efficient**: Proper cleanup of old textures to prevent memory leaks

## 📋 Requirements

- Minecraft 1.20.1
- Fabric Loader 0.16.12+
- Fabric API 0.92.6+
- Java 17+

## 🚀 Installation

### For Players

1. Download the latest `.jar` file from releases
2. Place it in your `%minecraft%/mods` folder
3. Launch Minecraft with Fabric Loader
4. Press **K** in-game to open the skin manager

### For Developers

1. Clone this repository
2. Run `./gradlew build` (or `gradlew.bat build` on Windows)
3. Find the built JAR in `build/libs/`

## 🎮 Usage

1. **Open the GUI**: Press **K** in-game
2. **Drag & Drop**: Drag a PNG file from your file explorer into the drop zone
3. **Alternative**: Click "Pick File" to browse for PNG files
4. **Apply**: Click "Apply Skin" to apply the skin
5. **Clear**: Use "Clear Skin" to remove the custom skin
6. **Close**: Click "Close" or press ESC

## 🛠️ Building from Source

### Prerequisites

- Java 17 or higher
- Gradle (included in the project)

### Build Commands

```bash
# Build the mod
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean build
./gradlew clean build

# Generate sources JAR
./gradlew sourcesJar
```

### Build Output

The built mod will be available in:
- `build/libs/felixskin-1.1.4.jar` - Main mod JAR
- `build/libs/felixskin-1.1.4-sources.jar` - Source code JAR

## 🏗️ Project Structure

```
src/
├── client/java/xyz/felixcraft/felixskin/
│   ├── FelixSkinClient.java          # Main mod entry point
│   ├── gui/
│   │   └── SkinManagerScreen.java    # Main GUI screen
│   ├── skin/
│   │   └── SkinManager.java          # Skin loading and management
│   ├── config/
│   │   └── FelixSkinConfig.java      # Configuration management
│   └── mixins/
│       └── PlayerSkinMixin.java      # Player rendering mixin
└── main/resources/
    ├── fabric.mod.json               # Mod metadata
    ├── felixskin.mixins.json         # Mixin configuration
    └── assets/felixskin/
        ├── lang/en_us.json           # Language file
        └── icon.png                  # Mod icon
```

## 🔧 Technical Details

### Mixin Integration

The mod uses Fabric Mixin to intercept:
- `AbstractClientPlayerEntity.getSkinTexture()` - Returns custom texture identifier
- `AbstractClientPlayerEntity.isSlim()` - Returns custom slim model flag

### Texture Management

- Custom skins are loaded as `NativeImage` and converted to `NativeImageBackedTexture`
- Textures are registered with unique identifiers: `felixskin:dynamic/{player-uuid}`
- Old textures are properly cleaned up to prevent memory leaks

### GLFW Integration

- Uses GLFW drop callback to receive file drops from the OS
- Safely handles file path encoding and validation
- Only accepts PNG files with reasonable size limits

## ⚠️ Troubleshooting

### Common Issues

1. **Mod not loading**: Ensure you have the correct Fabric Loader version
2. **K key not working**: Check if the key binding conflicts with other mods
3. **Skin not applying**: Verify the PNG file is valid and not corrupted
4. **Performance issues**: Large HD skins (>2048x2048) may impact performance

### Logs

Check the Minecraft logs for any error messages. The mod logs important events with the prefix `[FelixSkin]`.

## 📞 Support

- **GitHub**: https://github.com/edoren/SkinChanger
- **Discord**: Join our community for support
- **Issues**: Report bugs on GitHub Issues

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Made with ❤️ by Frame121**