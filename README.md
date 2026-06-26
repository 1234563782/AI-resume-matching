# AI Recruitment System

Spring Boot MVP for resume parsing, JD modeling, pgvector storage, candidate matching, and radar-chart visualization.

## Run

1. Create PostgreSQL database `ai_recruitment` and enable pgvector.
2. Set `DASHSCOPE_API_KEY`.
3. Start the app:

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`.

## APIs

- `POST /api/resumes` multipart field `file`: parse PDF/DOC/DOCX/TXT/image resume, extract profile, store embedding.
- `POST /api/jobs`: create JD from `{ "title": "...", "jdText": "..." }`.
- `POST /api/matches?resumeId=1&jobId=1`: calculate one resume-job match.
- `GET /api/matches/jobs/{jobId}/recommendations`: rank all resumes for one JD.

## Notes

The first version uses Tika for document text extraction and optional Tesseract OCR for images. Layout analysis is represented by `TextBlock` coordinates and can later be upgraded with OpenCV or LayoutParser when handling multi-column scans and complex templates.
