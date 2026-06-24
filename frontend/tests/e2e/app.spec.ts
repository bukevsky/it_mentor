import { expect, test } from "@playwright/test";
import { registerApiMocks, setStableClientState } from "./support/mock-api";
import { registerStompMock } from "./support/mock-stomp";

test.describe("IT Mentor UI", () => {
  test("renders guest home and auth screens", async ({ page }) => {
    await setStableClientState(page, false);
    await registerApiMocks(page);

    await page.goto("/");

    await expect(page.getByRole("heading", { name: "IT Mentor Platform" })).toBeVisible();
    await expect(page.getByRole("button", { name: "Аккаунт" })).toBeVisible();
    await expect(page).toHaveScreenshot("guest-home.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });

    await page.goto("/auth");
    await page.getByRole("tab", { name: "Регистрация" }).click();

    await expect(page.getByText("Создать аккаунт")).toBeVisible();
    await expect(page).toHaveScreenshot("auth-register.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });
  });

  test("lets authenticated user browse mentors and requests", async ({ page }) => {
    await setStableClientState(page, true);
    await registerApiMocks(page, { authenticated: true });
    await registerStompMock(page);

    await page.goto("/mentors");

    await expect(page.getByRole("heading", { name: "Найдите ментора под свою цель" })).toBeVisible();
    await expect(page.getByText("менторов найдено")).toBeVisible();
    await expect(page.getByRole("button", { name: "Связаться" })).toHaveCount(1);
    await page.getByRole("button", { name: "Связаться" }).click();
    await page.getByRole("button", { name: "Отправить заявку" }).click();
    await expect(page.getByText("Заявка отправлена.")).toBeVisible();
    await expect(page).toHaveScreenshot("mentors-directory.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });

    await page.goto("/requests");
    await expect(page.getByRole("heading", { name: "Заявки", exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Отозвать" }).click();
    await expect(page.getByText(/^Отозвано:/)).toBeVisible();
    await expect(page).toHaveScreenshot("requests-detail.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });
  });

  test("lets authenticated user work with chat and files", async ({ page }) => {
    await setStableClientState(page, true);
    await registerApiMocks(page, { authenticated: true });
    await registerStompMock(page);

    await page.goto("/chat");

    await expect(page.getByText("Воронова Анна").first()).toBeVisible();
    await page.getByRole("button", { name: /Воронова Анна/ }).click();
    await page.getByPlaceholder("Сообщение").fill("Сообщение через STOMP");
    await page.getByRole("button", { name: "Отправить" }).click();
    await expect(page.getByText("Сообщение отправлено.")).toBeVisible();
    await expect(page).toHaveScreenshot("chat-flow.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });

    await page.goto("/files");

    await page.locator('input[type="file"]').first().setInputFiles({
      name: "resume.pdf",
      mimeType: "application/pdf",
      buffer: Buffer.from("%PDF-1.4 test")
    });
    await page.getByRole("button", { name: "Загрузить" }).first().click();
    await expect(page.getByText("resume.pdf")).toBeVisible();
    await expect(page).toHaveScreenshot("files-upload.png", {
      fullPage: true,
      animations: "disabled",
      caret: "hide"
    });
  });
});
