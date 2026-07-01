# StudyPlatform Judge Sandbox

This is a standalone judge service for C++ submissions. It implements the backend sandbox contract:

```http
POST /judge
```

It compiles C++ source code and runs it against each test case in an isolated working directory. For production, run this service inside Docker or a stronger sandbox. Do not execute untrusted code inside the Spring Boot backend.

## Local Requirements

Local mode requires:

- Node.js 22+
- A C++ compiler. This project can use the bundled compiler under `toolchains/mingw64`.

On this machine, Dev-C++ MinGW64 has been copied to:

```text
judge-sandbox/toolchains/mingw64
```

That directory is ignored by Git because it is large.

## Start Locally

```powershell
cd judge-sandbox
npm start
```

Optional environment variables:

```powershell
$env:PORT='9000'
$env:CPP_COMPILER='g++'
$env:JUDGE_WORK_DIR='D:\tmp\studyplatform-judge'
npm start
```

The default compiler path on Windows is:

```text
judge-sandbox/toolchains/mingw64/bin/g++.exe
```

Set `CPP_COMPILER` only if you want to override it.

The bundled Dev-C++ compiler may be old. The sandbox tries `-std=c++17`, then falls back to `-std=c++11`, then falls back to no explicit standard flag.

## Start With Docker

```powershell
cd judge-sandbox
docker build -t studyplatform-judge-sandbox .
docker run --rm -p 9000:9000 --network none --memory 512m --cpus 1 studyplatform-judge-sandbox
```

If the backend is outside the same Docker network, do not use `--network none` for the service container itself. Instead isolate the per-submission execution layer with a dedicated worker model.

## Backend Configuration

Set:

```properties
oj.sandbox-url=http://localhost:9000
```

Then submit with language:

```text
cpp
```

## Supported C++ Template

```cpp
#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    long long a, b;
    cin >> a >> b;
    cout << a + b << '\n';
    return 0;
}
```

## Security Notes

This implementation is a development sandbox. Production judging should add:

- Container or VM isolation per run.
- CPU, memory, process, file size, and wall-clock limits enforced by the OS.
- Network isolation.
- Read-only root filesystem and dedicated temporary working directory.
- Queue-based execution to prevent request bursts from exhausting resources.
