# Design System Reference

Этот каталог нужен как рабочая шпаргалка по визуальной базе проекта.

Источник собран из двух мест:

- Figma файл: `https://www.figma.com/design/LPOwcMJiSEYowZahc8iRj2/...`
- установленный пакет `conductor@2.2.0`

Что удалось подтвердить напрямую через Figma MCP:

- файл `LPOwcMJiSEYowZahc8iRj2`
- цветовая страница по узлу `2190:174010`
- cover-структура страницы `Colors`
- именование палитры `Primary / Gray / Green / Yellow / Red`

Дополнительно пользователь передал node-ссылки этого блока, которые нужно дочитать после сброса лимита MCP:

- `2766:77316`
- `3291:2792`
- `4556:1693`
- `2766:77317`

Что восстановлено по локальному пакету `conductor`:

- точные значения color tokens
- typography scale
- spacing scale
- radius scale
- breakpoints и grid rules
- semantic theme mapping
- theme names и scope переопределений
- layout constants: sidebar, header, footer, icon sizes
- utility-классы для `gap`, `margin`, `padding`, `flex`

Ограничение:

- Figma MCP упёрся в лимит вызовов для текущего `View seat`, поэтому reference ниже не является полным dump всех pages файла.
- На момент обновления этой документации 2026-04-09 прямой повторный вызов к node `2766:77317`, `4556:1693`, `3291:2792` снова вернул тот же rate limit.
- Для полного повторного съёма foundations из Figma нужно либо больше лимита, либо вручную открыть/дать ссылки на страницы `Typography`, `Spacing`, `Effects`, `Radius`.

Содержимое:

- [colors.md](./colors.md) — палитра, theme coverage и semantic color aliases
- [foundations.md](./foundations.md) — typography, spacing, radii, layout constants, grid и utility classes

Быстрые исходники в проекте:

- `frontend/node_modules/conductor/dist/styles/variables.scss`
- `frontend/node_modules/conductor/dist/styles/fonts.scss`
- `frontend/node_modules/conductor/dist/styles/grid.scss`
- `frontend/node_modules/conductor/dist/styles/themes.scss`
- `frontend/src/styles/conductor-lite.css`

Если потом получится снова пройти по Figma MCP без лимита, этот каталог нужно обновить и разнести по отдельным страницам:

- `typography.md`
- `spacing.md`
- `effects.md`
- `radius.md`
