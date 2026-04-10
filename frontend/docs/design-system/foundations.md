# Foundations

## Typography

Источник:

- `frontend/node_modules/conductor/dist/styles/variables.scss`
- `frontend/node_modules/conductor/dist/styles/fonts.scss`

Базовый шрифт:

- `Open Sans`
- веса, найденные в пакете: `400`, `600`
- стили: `normal`, `italic`

### Type Scale

| Token | Font size | Line height |
| --- | --- | --- |
| `small-information` | `11px` | `16px` |
| `label-text` | `13px` | `16px` |
| `paragraph` | `15px` | `24px` |
| `headline-title` | `18px` | `24px` |
| `h3` | `20px` | `24px` |
| `h2` | `24px` | `32px` |
| `h1` | `28px` | `40px` |
| `h0` | `32px` | `40px` |
| `pr` | `36px` | `48px` |

Практический вывод:

- система строится на 8px baseline
- line-height в большинстве случаев кратен `8px`
- заголовки и абзацы уже готовы к переводу в app-level typography tokens

## Spacing

Базовый модуль:

- `$modul = 8px`

Scale:

| Token | Value |
| --- | --- |
| `micro` | `4px` |
| `xs` | `8px` |
| `s` | `16px` |
| `m` | `24px` |
| `l` | `32px` |
| `xl` | `40px` |
| `xxl` | `48px` |
| `huge` | `64px` |

## Radius

Базовая единица:

- `$radius = 8px`

Derived scale:

| Token | Value |
| --- | --- |
| `radius-xs` | `4px` |
| `radius-s` | `8px` |
| `radius-m` | `16px` |
| `radius-l` | `24px` |
| `radius-round` | `888px` |

## Heights

| Token | Value |
| --- | --- |
| `height-size-xs` | `24px` |
| `height-size-s` | `32px` |
| `height-size-m` | `40px` |
| `height-size-l` | `48px` |
| `height-size-xxl` | `56px` |

Дополнительно для toggle:

| Token | Value |
| --- | --- |
| `toggle-container-width-s` | `32px` |
| `toggle-container-height-s` | `16px` |
| `toggle-container-width-m` | `44px` |
| `toggle-container-height-m` | `24px` |
| `toggle-size-s` | `10px` |
| `toggle-size-m` | `16px` |
| `toggle-translateX-s` | `16px` |
| `toggle-translateX-m` | `18px` |

## Layout Constants

Дополнительные размеры из `variables.scss`, которые полезны для shell/layout слоя:

| Token | Value |
| --- | --- |
| `sidebar-width-short` | `72px` |
| `sidebar-width` | `268px` |
| `header-height` | `48px` |
| `footer-height` | `48px` |
| `icon-size-s` | `16px` |
| `icon-size-m` | `24px` |

## Grid

Источник:

- `frontend/node_modules/conductor/dist/styles/grid.scss`
- `frontend/node_modules/conductor/README.md`

Базовые grid custom properties:

- `--v-grid-modul = 8px`
- `--v-grid-gutter = 16px`

### Breakpoints

| Token | Min width |
| --- | --- |
| `tablet` | `475px` |
| `nettop` | `810px` |
| `laptop` | `1000px` |
| `hd` | `1200px` |
| `hdplus` | `1400px` |
| `fullhd` | `1800px` |

### Default Columns Per Row

| Width | Columns |
| --- | --- |
| `< 475px` | `2` |
| `>= 475px` | `3` |
| `>= 810px` | `4` |
| `>= 1000px` | `5` |
| `>= 1200px` | `6` |
| `>= 1400px` | `7` |
| `>= 1800px` | `8` |

Важно для `VCol`:

- `:laptop="1"` значит `1` колонка в ряду, то есть блок `100%`
- `:laptop="2"` значит `2` колонки в ряду, то есть блок `50%`
- `:laptop="4"` значит `4` колонки в ряду, то есть блок `25%`

Это не bootstrap-span логика.

### Gutter Mapping

| Prop | Real value |
| --- | --- |
| `0.5` | `4px` |
| `1` | `8px` |
| `1.5` | `12px` |
| `2` | `16px` |

## Utility Classes

Собранный `conductor-lite.css` даёт ещё один слой reference, который удобно знать при разборе старых макетов и legacy-верстки:

- gap utilities: `.ga-0` ... `.ga-16` дают шаг от `0px` до `64px` с инкрементом `4px`
- spacing utilities: `.mt-*`, `.mr-*`, `.mb-*`, `.ml-*`, `.pt-*`, `.pr-*`, `.pb-*`, `.pl-*`, `.ma-*`, `.pa-*` идут по шкале `4px` и в текущем CSS доходят до `40px`
- flex utilities: `.d-flex`, `.d-inline-flex`, `.flex-row`, `.flex-column`, `.flex-wrap`, `.justify-center`, `.align-center`, `.flex-grow-1`, `.flex-shrink-0`
- order/alignment utilities: `.order-first`, `.order-last`, `.align-self-*`, `.align-content-*`
- grid helpers: `.grid-block`, `.grid-two-block`, `.grid-three-block`, `.grid-two-three-block`
- legacy aliases тоже присутствуют: `.fl`, `.fl_col`, `.fl_gap1` ... `.fl_gap6`, `.fl_j_sb`, `.fl_a_c`

Практический вывод:

- в пакете уже есть utility-first слой поверх токенов
- при переносе старых экранов из Figma полезно сначала проверить, нет ли для нужной раскладки готового utility-класса
- эти классы не заменяют app-level design tokens, но ускоряют чтение и миграцию существующей верстки

## Semantic Text Roles

По `themes.scss`:

- `typo-primary` -> `gray000-C`
- `typo-secondary` -> `primary700`
- `typo-default` -> `gray900`
- `typo-system` -> `gray500`
- `typo-ghost` -> `gray200`
- `typo-disabled` -> `gray300`
- `typo-error` -> `red600`
- `typo-warning` -> `yellow600`
- `typo-success` -> `green600`
- `typo-drag` -> `purple600`

## Controls Mapping

Полезно для сборки app-level tokens:

- `control-btn-primary-bg-default` -> `primary700`
- `control-btn-primary-bg-hover` -> `primary500`
- `control-btn-primary-bg-pressed` -> `primary750`
- `control-btn-primary-bg-disabled` -> `gray150`
- `control-btn-secondary-bg-default` -> `gray000`
- `control-btn-secondary-bg-hover` -> `gray150-A`
- `control-btn-secondary-bg-hover2` -> `gray150`
- `control-btn-secondary-bg-pressed` -> `gray200`
- `control-btn-secondary-bg-disabled` -> `gray150`
- `control-btn-secondary-border-default` -> `gray250`
- `control-btn-secondary-typo-default` -> `gray700`
- `control-btn-secondary-typo-hover` -> `gray600`
- `control-btn-secondary-typo-disabled` -> `gray300`
- `control-border-default` -> `gray700`
- `control-border-disabled` -> `gray200`

## Shadows / Effects

Из foundation-файлов `conductor` явные shadow tokens не вытащились.

Что есть сейчас в самом проекте:

- `frontend/src/style.scss`
- `--shell-shadow: 0 28px 60px rgba(24, 41, 67, 0.12)`
- dark mode: `0 28px 60px rgba(0, 0, 0, 0.34)`

Это уже app-level решения, а не подтверждённые design-system tokens из Figma.

Если нужен точный reference по `shadows`, `effects` и `blur`, надо отдельно дочитать соответствующую Figma page после снятия лимита MCP.
