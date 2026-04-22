# Image Assets

Put general UI background images in this folder.

Supported formats are normal browser image formats such as PNG, JPG, JPEG, or WEBP, but the current code expects the PNG file name below.

Do not use Chinese file names.

## Folder

```text
src/main/resources/static/assets/images
```

## Required File Names

- `main_menu_bg.png` - 主菜单背景图

## Usage

The main menu uses:

```text
/assets/images/main_menu_bg.png
```

Recommended image shape: horizontal 16:9 or wider. The page uses `background-size: cover`, so the edges may be cropped on very narrow or very wide screens.
