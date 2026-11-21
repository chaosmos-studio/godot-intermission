import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  site: "https://chaosmos.studio",
  base: "/intermission",
  trailingSlash: "always",
  build: { format: "directory" },
  integrations: [
    starlight({
      title: "Intermission",
      description: "Unity LevelPlay and Unity Ads for Godot 4.7 games on Android.",
      logo: { light: "./src/assets/mark-light.svg", dark: "./src/assets/mark-dark.svg", alt: "" },
      favicon: "/favicon.svg",
      social: [{ icon: "github", label: "GitHub", href: "https://github.com/chaosmos-studio/godot-intermission" }],
      editLink: { baseUrl: "https://github.com/chaosmos-studio/godot-intermission/edit/main/site/" },
      customCss: ["@fontsource-variable/geist", "@fontsource-variable/geist-mono", "./src/styles/theme.css"],
      components: {
        ThemeProvider: "./src/components/ThemeProvider.astro",
        ThemeSelect: "./src/components/ThemeSelect.astro",
        Footer: "./src/components/Footer.astro",
      },
      sidebar: [
        { label: "Start", items: ["install", "quick-start"] },
        {
          label: "Guides",
          items: ["guides/full-screen-ads", "guides/banners", "guides/consent", "guides/test-mode", "guides/misbehaving-ads"],
        },
        { label: "Reference", items: ["reference/methods", "reference/signals", "reference/enums", "reference/how-it-works"] },
        { label: "Help", items: ["troubleshooting", "updating", "changelog"] },
      ],
    }),
  ],
});
