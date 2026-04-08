# DonutCombatLog - source-buildable repo

Đây là bản repo **build được từ source** để bạn test và customize dễ hơn trong VS Code.

## Mục tiêu của repo này

- giữ logic DonutCombatLog gần với jar gốc
- build ra file `.jar` hoàn chỉnh để thả vào thư mục `plugins/`
- tránh phụ thuộc compile-time vào plugin `DonutDatabase`
- hỗ trợ Paper/Folia bằng scheduler phù hợp cho 1.21.x

## Những điểm đã đổi so với jar gốc

1. `DonutDatabase` được gọi qua reflection trong `DonutDatabaseAdapter`, để project compile được dù bạn không có source/jar của plugin đó trong workspace.
2. `FoliaLib` được thay bằng `SchedulerBridge` dùng API scheduler của Paper/Folia trực tiếp.
3. State map đổi sang `ConcurrentHashMap` để an toàn hơn khi chạy trên Folia.

## Build trong VS Code

### Cách nhanh nhất

- Windows: chạy `build.ps1`
- Linux/macOS: chạy `build.sh`

Hai script này sẽ tự tải Maven 3.9.14 vào thư mục `.tools/` nếu máy bạn chưa có Maven.

File build xong nằm ở:

```text
target/DonutCombatLog-3.1.2-custom.jar
```

### Dùng task của VS Code

- Mở folder này bằng VS Code
- Cài `Extension Pack for Java`
- Nhấn `Terminal` -> `Run Build Task`
- Chọn task phù hợp với hệ điều hành

## Yêu cầu môi trường

- JDK 21
- internet lần đầu build để Maven tải dependency

## Chỉnh sửa code ở đâu

- plugin chính: `src/main/java/me/serbob/donutcombatlog/DonutCombatLog.java`
- adapter DonutDatabase: `src/main/java/me/serbob/donutcombatlog/internal/DonutDatabaseAdapter.java`
- scheduler: `src/main/java/me/serbob/donutcombatlog/internal/SchedulerBridge.java`
- config/plugin yml: `src/main/resources/`

## Chạy thử trên server

1. build jar
2. copy `target/DonutCombatLog-3.1.2-custom.jar` vào thư mục `plugins/`
3. đảm bảo server có `DonutDatabase`
4. restart server

## Ghi chú

`DonutBounty` vẫn được giữ trong `plugin.yml` để bám theo jar gốc. Nếu server của bạn không cần dependency đó, bạn có thể tự bỏ khỏi `plugin.yml`.
