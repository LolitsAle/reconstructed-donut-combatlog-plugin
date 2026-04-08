# DonutCombatLog - source-buildable repo

Đây là bản repo **build được từ source** để test và customize dễ hơn trong VS Code.

## Mục tiêu của repo này

- giữ logic DonutCombatLog gần với jar gốc
- build ra file `.jar` hoàn chỉnh để thả vào thư mục `plugins/`
- tránh phụ thuộc compile-time vào plugin `DonutDatabase`
- hỗ trợ Paper/Folia bằng scheduler phù hợp cho 1.21.x

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
