import type { Page, Route } from "@playwright/test";

type MockOptions = {
  authenticated?: boolean;
};

type RequestRecord = {
  id: number;
  studentProfileId: number;
  mentorProfileId: number;
  studentProfile: {
    id: number;
    firstName: string;
    lastName: string;
  };
  mentorProfile: {
    id: number;
    firstName: string;
    lastName: string;
    position: string | null;
  };
  direction: "STUDENT_TO_MENTOR" | "MENTOR_TO_STUDENT";
  status: string;
  goalType: string;
  message: string;
  clarificationNote: string | null;
  reason: string | null;
  createdAt: string;
  respondedAt: string | null;
  completedAt: string | null;
};

const now = "2026-04-07T10:00:00.000Z";
const token = "playwright-token";

const user = {
  id: 17,
  email: "student@example.com",
  roles: ["STUDENT"],
  status: "ACTIVE"
};

const cities = [
  { id: 1, name: "Москва", region: "Москва", country: "Россия" },
  { id: 2, name: "Санкт-Петербург", region: "СЗФО", country: "Россия" }
];

const skills = [
  { id: 1, name: "Java", category: "Backend" },
  { id: 2, name: "Spring", category: "Backend" },
  { id: 3, name: "SQL", category: "Data" }
];

const languages = [
  { id: 1, name: "English", code: "en" },
  { id: 2, name: "Русский", code: "ru" }
];

const interactionTypes = [
  { id: 1, name: "Чат", description: "Текстовое общение" },
  { id: 2, name: "Созвон", description: "Онлайн-встречи" }
];

const studentProfile = {
  id: 101,
  userId: 17,
  firstName: "Ярослав",
  lastName: "Шилов",
  middleName: null,
  phone: "+7 999 000-00-00",
  city: cities[0],
  desiredPosition: "Backend Developer",
  hoursPerWeek: 20,
  availableFrom: "2026-04-10",
  about: "Развиваюсь в backend и ищу ментора для системной практики.",
  max: "@yaroslav",
  employmentTypes: ["INTERNSHIP"],
  workFormats: ["REMOTE"],
  educations: [],
  languages: [{ id: 1, language: languages[0], level: "B2" }],
  skills: [{ id: 1, skill: skills[0], level: "INTERMEDIATE" }],
  resumeFileId: 801
};

const mentorProfile = {
  id: 201,
  userId: 31,
  firstName: "Анна",
  lastName: "Воронова",
  middleName: null,
  position: "Senior Backend Developer",
  department: "Platform",
  city: cities[0],
  phone: "+7 900 100-20-30",
  max: "@anna_backend",
  description: "Помогаю собирать backend-стек и проходить путь до стажировки.",
  expectations: "Готовность выполнять договорённости и двигаться по плану.",
  canHelpWith: "Java, Spring, архитектура, подготовка к интервью.",
  mentoringType: "INTERNSHIP",
  mentoringChannel: "MIXED",
  mentoringFrequency: "2 раза в неделю",
  mentoringDuration: "THREE_MONTHS",
  menteeLimit: 3,
  recruitmentStatus: "OPEN",
  skills: [
    { id: 1, skill: skills[0], level: "CONFIDENT" },
    { id: 2, skill: skills[1], level: "CONFIDENT" }
  ]
};

const mentorCards = [
  {
    id: 201,
    userId: 31,
    firstName: "Анна",
    lastName: "Воронова",
    middleName: null,
    position: "Senior Backend Developer",
    department: "Platform",
    city: cities[0],
    mentoringType: "INTERNSHIP",
    mentoringChannel: "MIXED",
    mentoringDuration: "THREE_MONTHS",
    menteeLimit: 3,
    recruitmentStatus: "OPEN",
    skills: [
      { id: 1, skill: skills[0], level: "CONFIDENT" },
      { id: 2, skill: skills[1], level: "CONFIDENT" }
    ]
  },
  {
    id: 202,
    userId: 32,
    firstName: "Илья",
    lastName: "Поляков",
    middleName: null,
    position: "Tech Lead",
    department: "Core",
    city: cities[1],
    mentoringType: "PROJECT",
    mentoringChannel: "CHAT",
    mentoringDuration: "FLEXIBLE",
    menteeLimit: 2,
    recruitmentStatus: "OPEN",
    skills: [{ id: 3, skill: skills[2], level: "CONFIDENT" }]
  }
];

const uploadedFile = {
  id: 901,
  originalFilename: "resume.pdf",
  contentType: "application/pdf",
  size: 20480,
  fileType: "RESUME",
  uploadedAt: now
};

const json = async (route: Route, body: unknown, status = 200) => {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body)
  });
};

const noContent = async (route: Route) => {
  await route.fulfill({ status: 204, body: "" });
};

const error = async (route: Route, status: number, message: string, path: string) => {
  await json(route, {
    timestamp: now,
    status,
    error: "API_ERROR",
    message,
    path
  }, status);
};

export const setStableClientState = async (page: Page, authenticated = false) => {
  await page.addInitScript(([nextToken]) => {
    window.localStorage.setItem("theme", "default-light");

    if (nextToken) {
      window.sessionStorage.setItem("it-mentor.access-token", nextToken);
      return;
    }

    window.sessionStorage.removeItem("it-mentor.access-token");
  }, [authenticated ? token : null]);
};

export const registerApiMocks = async (page: Page, options: MockOptions = {}) => {
  const state = {
    authenticated: Boolean(options.authenticated),
    requests: [
      {
        id: 301,
        studentProfileId: 101,
        mentorProfileId: 201,
        studentProfile: {
          id: 101,
          firstName: "Ярослав",
          lastName: "Шилов"
        },
        mentorProfile: {
          id: 201,
          firstName: "Анна",
          lastName: "Воронова",
          position: "Senior Backend Developer"
        },
        direction: "STUDENT_TO_MENTOR",
        status: "SENT",
        goalType: "INTERNSHIP",
        message: "Хочу получить сопровождение по backend-стеку и плану развития.",
        clarificationNote: null,
        reason: null,
        createdAt: now,
        respondedAt: null,
        completedAt: null
      }
    ] as RequestRecord[],
    chat: {
      id: 401,
      mentoringRequestId: 301,
      studentUserId: 17,
      mentorUserId: 31,
      createdAt: now
    },
    files: [] as (typeof uploadedFile)[],
    messages: [
      {
        id: 501,
        chatId: 401,
        senderUserId: 31,
        clientMessageId: "00000000-0000-4000-8000-000000000501",
        body: "Давайте начнём с резюме и текущего опыта.",
        attachment: null,
        deliveryStatus: "READ",
        deliveredAt: now,
        readAt: now,
        createdAt: now
      },
      {
        id: 502,
        chatId: 401,
        senderUserId: 17,
        clientMessageId: "00000000-0000-4000-8000-000000000502",
        body: "Отправил резюме и список тем, которые хочу закрыть в первую очередь.",
        attachment: {
          fileId: 901,
          originalFilename: "resume.pdf",
          contentType: "application/pdf",
          size: 20480
        },
        deliveryStatus: "READ",
        deliveredAt: "2026-04-07T10:05:00.000Z",
        readAt: "2026-04-07T10:05:00.000Z",
        createdAt: "2026-04-07T10:05:00.000Z"
      }
    ]
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const method = request.method();
    const url = new URL(request.url());

    if (!url.pathname.startsWith("/api/")) {
      await route.fallback();
      return;
    }

    const path = url.pathname.replace(/^\/api/, "");

    if (method === "GET" && path === "/actuator/health") {
      return json(route, {
        status: "UP",
        components: {
          db: { status: "UP" },
          readinessState: { status: "UP" }
        }
      });
    }

    if (method === "GET" && path === "/dictionaries/cities") {
      return json(route, cities);
    }

    if (method === "GET" && path === "/dictionaries/skills") {
      return json(route, skills);
    }

    if (method === "GET" && path === "/dictionaries/languages") {
      return json(route, languages);
    }

    if (method === "GET" && path === "/dictionaries/interaction-types") {
      return json(route, interactionTypes);
    }

    if (method === "POST" && path === "/auth/login") {
      state.authenticated = true;
      return json(route, {
        accessToken: token,
        tokenType: "Bearer",
        user
      });
    }

    if (method === "POST" && path === "/auth/register") {
      return json(route, {
        id: 99,
        email: "new.student@example.com",
        roles: ["STUDENT"]
      }, 201);
    }

    if (method === "POST" && path === "/auth/password/forgot") {
      return noContent(route);
    }

    if (method === "POST" && path === "/auth/password/reset") {
      return noContent(route);
    }

    if (method === "GET" && path === "/auth/me") {
      if (!state.authenticated) {
        return error(route, 401, "Требуется авторизация.", path);
      }

      return json(route, user);
    }

    if (method === "GET" && path === "/profile/me") {
      if (!state.authenticated) {
        return error(route, 401, "Требуется авторизация.", path);
      }

      return json(route, {
        role: "STUDENT",
        profileId: 101,
        profileExists: true
      });
    }

    if (method === "GET" && path === "/profile/student/me") {
      return json(route, studentProfile);
    }

    if (method === "PUT" && path === "/profile/student") {
      return json(route, studentProfile);
    }

    if (method === "GET" && path === "/profile/mentor/me") {
      return json(route, mentorProfile);
    }

    if (method === "PUT" && path === "/profile/mentor") {
      return json(route, mentorProfile);
    }

    if (method === "GET" && path === "/profiles/mentors") {
      return json(route, {
        content: mentorCards,
        page: 0,
        size: 24,
        totalElements: mentorCards.length,
        totalPages: 1,
        last: true
      });
    }

    if (method === "POST" && path === "/mentoring/requests") {
      const payload = request.postDataJSON() as {
        mentorProfileId: number;
        goalType: string;
        message: string;
      };
      const selectedMentor =
        mentorCards.find((mentor) => mentor.id === payload.mentorProfileId) ?? mentorCards[0];
      const created = {
        ...state.requests[0],
        id: state.requests.length + 301,
        mentorProfileId: selectedMentor.id,
        mentorProfile: {
          id: selectedMentor.id,
          firstName: selectedMentor.firstName,
          lastName: selectedMentor.lastName,
          position: selectedMentor.position
        },
        direction: "STUDENT_TO_MENTOR" as const,
        status: "SENT",
        goalType: payload.goalType,
        message: payload.message
      };

      state.requests.unshift(created);
      return json(route, created, 201);
    }

    if (method === "GET" && path === "/mentoring/requests") {
      return json(route, {
        content: state.requests,
        page: 0,
        size: 20,
        totalElements: state.requests.length,
        totalPages: 1,
        last: true
      });
    }

    if (path.startsWith("/mentoring/requests/")) {
      const match = path.match(/^\/mentoring\/requests\/(\d+)(?:\/(.+))?$/);
      const requestId = match ? Number(match[1]) : null;
      const action = match?.[2] ?? "";
      const target = state.requests.find((item) => item.id === requestId) ?? state.requests[0];

      if (method === "GET" && requestId) {
        return json(route, target);
      }

      if (method === "PUT" && action) {
        if (action === "view") target.status = "REVIEWING";
        if (action === "accept") target.status = "ACCEPTED";
        if (action === "needs-clarification") target.status = "NEEDS_CLARIFICATION";
        if (action === "cancel") target.status = "CANCELLED";
        if (action === "complete") target.status = "COMPLETED";
        if (action === "reject") target.status = "REJECTED";

        return json(route, target);
      }
    }

    if (method === "GET" && path === "/chats") {
      return json(route, {
        content: [state.chat],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        last: true
      });
    }

    if (method === "GET" && path === "/chats/by-request/301") {
      return json(route, state.chat);
    }

    if (method === "GET" && path === "/chats/401") {
      return json(route, state.chat);
    }

    if (method === "GET" && path === "/chats/401/messages") {
      return json(route, {
        content: state.messages,
        page: 0,
        size: 40,
        totalElements: state.messages.length,
        totalPages: 1,
        last: true
      });
    }

    if (method === "GET" && path === "/chats/401/messages/sync") {
      return json(route, []);
    }

    if (method === "GET" && path === "/presence/31") {
      return json(route, { userId: 31, status: "online", lastSeenAt: null });
    }

    if (method === "GET" && path === "/files") {
      return json(route, {
        content: state.files,
        page: 0,
        size: 200,
        totalElements: state.files.length,
        totalPages: state.files.length ? 1 : 0,
        last: true
      });
    }

    if (
      method === "POST" &&
      ["/files/resume", "/files/portfolio", "/files/avatar", "/files/chat-attachment"].includes(path)
    ) {
      state.files = [uploadedFile];
      return json(route, uploadedFile, 201);
    }

    return error(route, 404, `Mock not found for ${method} ${path}`, path);
  });
};
