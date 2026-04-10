# Colors

## Figma Reference

Подтверждённая страница:

- `Colors`
- узел: `2190:174010`
- файл: `LPOwcMJiSEYowZahc8iRj2`

По Figma cover и локальным токенам видно, что базовая палитра строится вокруг таких групп:

- `Primary`
- `Gray`
- `Green`
- `Yellow`
- `Red`
- `Blue`
- `Purple`
- `Attention`
- `Color001` ... `Color016`

## Theme Coverage

В установленном `conductor` реально присутствуют такие theme scopes:

- `:root` — базовый набор, почти совпадает с `default-light`, но без отдельных theme-only токенов вроде `gray000-B`; также содержит extra accent set `color001` ... `color016`
- `default-light` — основная светлая тема
- `default-dark` — основная тёмная тема
- `ocrv-light` — light-only брендовая тема
- `grv-light` — light-only брендовая тема

Практический вывод по `variables.scss`:

- отдельные brand dark темы в пакете не найдены
- `ocrv-light` и `grv-light` переопределяют только часть базовой палитры
- не переопределённые токены в brand themes наследуются из `:root`

## Default Light

| Token | Value |
| --- | --- |
| `primary100` | `#DEF1FC` |
| `primary200` | `#CDE5F3` |
| `primary250` | `#B2D8EE` |
| `primary300` | `#A1CBE2` |
| `primary500` | `#1975AA` |
| `primary700` | `#0066A1` |
| `primary750` | `#005C91` |
| `gray000-C` | `#FFFFFF` |
| `gray000-B` | `#FFFFFF` |
| `gray000` | `#FFFFFF` |
| `gray100` | `#F6F6F6` |
| `gray150-A` | `#2B39470F` |
| `gray150` | `#F1F3F5` |
| `gray200` | `#DAE1E8` |
| `gray250` | `#D4DAE1` |
| `gray300` | `#AAB6C4` |
| `gray500` | `#71859C` |
| `gray600` | `#42505F` |
| `gray700` | `#2D3C4D` |
| `gray900` | `#141A22` |
| `gray900-C` | `#141A22` |
| `green100` | `#F2FBF2` |
| `green600` | `#04B800` |
| `yellow100` | `#FEF9F2` |
| `yellow600` | `#D3A500` |
| `red100` | `#FEF4F4` |
| `red600-A` | `#FF000062` |
| `red600` | `#E21A1A` |
| `blue100` | `#F4FBFE` |
| `blue600` | `#32B0FA` |
| `purple100` | `#FAEFFF` |
| `purple600` | `#C632FA` |
| `attention100` | `#FFF5F2` |
| `attention600` | `#FF5722` |

## Default Dark

| Token | Value |
| --- | --- |
| `primary100` | `#062537` |
| `primary200` | `#062B40` |
| `primary250` | `#03314B` |
| `primary300` | `#023755` |
| `primary500` | `#005C91` |
| `primary700` | `#0074B8` |
| `primary750` | `#008CDD` |
| `gray000-C` | `#FFFFFF` |
| `gray000-B` | `#575757` |
| `gray000` | `#23262B` |
| `gray100` | `#1C1E21` |
| `gray150-A` | `#BDDDFF0F` |
| `gray150` | `#2F3339` |
| `gray200` | `#525860` |
| `gray250` | `#606A76` |
| `gray300` | `#7D8A98` |
| `gray500` | `#8DA1B5` |
| `gray600` | `#B4C7DB` |
| `gray700` | `#DEE9F3` |
| `gray900` | `#FFFFFF` |
| `gray900-C` | `#141A22` |
| `green100` | `#1B271F` |
| `green600` | `#04B800` |
| `yellow100` | `#27261F` |
| `yellow600` | `#D3A500` |
| `red100` | `#281E21` |
| `red600-A` | `#C96060A1` |
| `red600` | `#FF3C3C` |
| `blue100` | `#1D272E` |
| `blue600` | `#32B0FA` |
| `purple100` | `#261F2E` |
| `purple600` | `#C632FA` |
| `attention600` | `#E05232` |

## Brand Variants

### OCRV Light

| Token | Value |
| --- | --- |
| `primary100` | `#E6F0FD` |
| `primary200` | `#CCE0F9` |
| `primary250` | `#B3D1F6` |
| `primary300` | `#99C1F3` |
| `primary500` | `#257EEC` |
| `primary700` | `#0065E2` |
| `primary750` | `#065CC7` |
| `gray150-A` | `rgba(6, 92, 199, 0.059)` |
| `gray150` | `#F0F5FC` |
| `gray250` | `#CDD7DC` |
| `gray300` | `#9DB0BB` |
| `gray900` | `#0C1117` |
| `red600` | `#ED202E` |

### GRV Light

| Token | Value |
| --- | --- |
| `primary100` | `#E2F4D8` |
| `primary200` | `#B8E4A4` |
| `primary250` | `#A1DB86` |
| `primary300` | `#6BC74C` |
| `primary500` | `#5FA742` |
| `primary700` | `#4C8636` |
| `primary750` | `#326121` |
| `gray150-A` | `rgba(6, 92, 199, 0.06)` |
| `gray150` | `#F0F5FC` |
| `gray250` | `#CDD7DC` |
| `gray300` | `#9DB0BB` |
| `gray900` | `#0C1117` |
| `red600` | `#ED202E` |

## Inheritance Notes

По `variables.scss` и собранному `conductor-lite.css` можно уверенно сказать:

- `gray000-B` существует только в `default-light` и `default-dark`
- `attention100` объявлен в `:root` и `default-light`, но не переопределяется в `default-dark`, `ocrv-light`, `grv-light`
- `attention600` переопределяется только в `default-dark`
- `color001` ... `color016` объявлены только в `:root`, значит brand themes используют root-значения без собственного набора accent colors
- `ocrv-light` и `grv-light` не переопределяют `attention*`, `gray000-B` и extra accent set

Это означает, что часть semantic aliases в брендовых темах опирается не на локальный brand palette, а на унаследованные root-значения.

## Extra Accent Set

Дополнительная палитра из `variables.scss`:

| Token | Value |
| --- | --- |
| `color001` | `#016BDC` |
| `color002` | `#C62828` |
| `color003` | `#008F7C` |
| `color004` | `#F56B00` |
| `color005` | `#7448DD` |
| `color006` | `#6FB80A` |
| `color007` | `#E7005B` |
| `color008` | `#F0C83C` |
| `color009` | `#8F17A1` |
| `color010` | `#A2AD7A` |
| `color011` | `#F149B8` |
| `color012` | `#3330CB` |
| `color013` | `#BF780C` |
| `color014` | `#4DBBFC` |
| `color015` | `#FF8EB3` |
| `color016` | `#08D0C4` |

## Semantic Mapping

`conductor` поверх палитры использует такие семантические роли:

- `bg-primary` -> `primary500`
- `bg-secondary` -> `gray100`
- `bg-default` -> `gray000`
- `bg-ghost` -> `gray200`
- `bg-disabled` -> `gray150`
- `bg-system` -> `gray500`
- `bg-border-default` -> `gray250`
- `bg-success-primary` -> `green600`
- `bg-success-secondary` -> `green100`
- `bg-warning-primary` -> `yellow600`
- `bg-warning-secondary` -> `yellow100`
- `bg-error-primary` -> `red600`
- `bg-error-secondary` -> `red100`
- `bg-info-primary` -> `blue600`
- `bg-info-secondary` -> `blue100`
- `bg-drag-primary` -> `purple600`
- `bg-drag-secondary` -> `purple100`
- `bg-attention-primary` -> `attention600`
- `bg-attention-secondary` -> `attention100`

## Derived Color Aliases

Дополнительно в `themes.scss` есть alias, которых не было в первой версии сводки:

- `typo-error-alfa` -> `red600-A`

Практический смысл:

- `red600-A` используется как полупрозрачный error-layer token
- `gray000-B` выглядит как отдельный surface token для специальных подложек, а не как часть основной gray scale
