import { defineConfig, presetUno } from "unocss"
import { presetBlock } from "unocss-preset-block"
import { presetPalette } from "unocss-preset-palette"

export default defineConfig({
  presets: [
    presetUno(),
    presetBlock(),
    presetPalette(
      {
        colorFormat: "rgb",
        themeColors: {
          "primary": [49, 168, 255],
          "primary-hover": [40, 120, 255],
          "primary-light": [49, 168, 255],
          "success": [74, 181, 165],
          "fail": [255, 72, 72],
          "warning": [255, 183, 20],
          "danger": [255, 100, 100],
          "back": [255, 255, 255],
          "grey": [153, 153, 153],
          "cardBg": "#283454",
        },
        cssVarName: "color-[name]",
      },
    ),
  ],
  shortcuts: {
    "page-wrapper": "pb-24 px-6 pt-4 relative h-full",
    "page-footer":
      "absolute flex justify-center bottom-0 left-0 right-0 items-center h-24",
  },
  rules: [
    [
      "text-justify-last",
      {
        "text-align-last": "justify",
        "text-align": "justify",
      },
    ],
    [
      /divider-(x|y)/,
      ([_]) => {
        return {
          "--divider-width": "0.5px",
          "--divider-color": "currentColor",
          "--divider-style": "solid",
          "border-width": "var(--divider-width)",
          "border-color": "var(--divider-color)",
          "border-style": "var(--divider-style)",
        }
      },
      {
        autocomplete: ["divider-(x|y)"],
      },
    ],
    [
      /divider-(dashed|dotted|solid)/,
      ([_, style]) => {
        return {
          "--divider-style": style,
        }
      },
      {
        autocomplete: ["divider-(dashed|dotted|solid)"],
      },
    ],
  ],
})
