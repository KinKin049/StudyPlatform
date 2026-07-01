# StudyPlatform

Study platform with a backend service and a Vue frontend.

## OJ Page

Start backend and frontend, then open:

```text
http://127.0.0.1:5173/oj.html
```

For the OJ file layout and file responsibilities, see:

```text
docs/oj-file-map.md
```

## C++ Judge Sandbox

The development C++ judge service lives in:

```text
judge-sandbox
```

It requires `g++` or Docker. Start it on port `9000`, then run the backend with:

```properties
oj.sandbox-url=http://localhost:9000
```
