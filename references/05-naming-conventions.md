# 네이밍 및 코딩 컨벤션 체크리스트

## 클래스 네이밍 체크리스트

| 레이어 | 패턴 | 확인 |
|--------|------|------|
| Controller | `{Resource}V{N}Controller` | - [ ] |
| ApiSpec | `{Resource}V{N}ApiSpec` | - [ ] |
| DTO 컨테이너 | `{Resource}V{N}Dto` | - [ ] |
| Facade | `{Entity}Facade` | - [ ] |
| Command | `{Entity}{Action}Command` | - [ ] |
| Info | `{Entity}{Detail}Info` | - [ ] |
| Service | `{Entity}Service` | - [ ] |
| Domain Service | `{Action}Service` | - [ ] |
| Entity | `{PascalCase}` | - [ ] |
| Value Object | `{Entity}{Attribute}` | - [ ] |
| Repository (if) | `{Entity}Repository` | - [ ] |
| Repository (impl) | `{Entity}RepositoryImpl` | - [ ] |
| JPA Repository | `{Entity}JpaRepository` | - [ ] |
| Domain Event | `{Entity}{Action}Event` | - [ ] |
| Unit Test | `{Entity}Test` | - [ ] |
| Integration Test | `{Entity}ServiceIntegrationTest` | - [ ] |
| E2E Test | `{Resource}V{N}ApiE2ETest` | - [ ] |

## 패키지 위치 체크리스트

| 파일 유형 | 경로 | 확인 |
|-----------|------|------|
| Controller, ApiSpec, Dto | `interfaces.api.{resource}` | - [ ] |
| Facade, Command, Info | `application.{entity}` | - [ ] |
| Entity, VO, Service, Repository(if) | `domain.{entity}` | - [ ] |
| RepositoryImpl, JpaRepository | `infrastructure.{entity}` | - [ ] |

## 어노테이션 체크리스트

| 클래스 | 필수 어노테이션 | 확인 |
|--------|---------------|------|
| Controller | `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor` | - [ ] |
| Facade | `@Component`, `@RequiredArgsConstructor`, `@Transactional(readOnly=true)` | - [ ] |
| Service | `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly=true)` | - [ ] |
| Entity | `@Entity`, `@Getter`, `@Table`, `@SQLRestriction`, `@NoArgsConstructor(PROTECTED)` | - [ ] |
| Value Object | `@Getter`, `@Embeddable`, `@EqualsAndHashCode`, `@NoArgsConstructor(PROTECTED)` | - [ ] |
| RepositoryImpl | `@Repository`, `@RequiredArgsConstructor` | - [ ] |

## DTO 변환 메서드 체크리스트

| 방향 | 메서드 | 확인 |
|------|--------|------|
| Entity → Info | `{Info}.of(entity, ...)` | - [ ] |
| Info → Response | `{Response}.from(info)` | - [ ] |

## API URL 체크리스트

- [ ] 기본: `/api/v1/{resource-복수형}`
- [ ] 단건: `/api/v1/{resource}/{id}`
- [ ] 액션: `/api/v1/{resource}/{action}` (예: `/points/charge`)

## 트랜잭션 체크리스트

- [ ] 클래스 레벨 `@Transactional(readOnly = true)` 기본
- [ ] 쓰기 메서드에만 `@Transactional` 오버라이드
- [ ] 비관적 락: `@Lock(PESSIMISTIC_WRITE)` + ID 오름차순 (데드락 방지)
- [ ] 교차 엔티티 락 순서: Product → Point

## 캐시 체크리스트

- [ ] 읽기 캐싱: `@Cacheable(value = "캐시명", key = "#id")`
- [ ] 쓰기 무효화: `@CacheEvict(value = "캐시명", key = "#id")`
- [ ] 캐시된 데이터를 변경하는 **모든 경로**에서 무효화 호출
- [ ] Redis TTL 기본 10분

## 새 도메인 추가 시 전체 체크리스트

```
1. Domain Layer
   - [ ] Entity 생성 (create 팩토리 + validate)
   - [ ] Value Object 생성 (필요 시)
   - [ ] Repository interface 생성
   - [ ] Service 생성

2. Infrastructure Layer
   - [ ] JpaRepository 생성
   - [ ] RepositoryImpl 생성

3. Application Layer
   - [ ] Info record 생성
   - [ ] Command record 생성 (필요 시)
   - [ ] Facade 생성

4. Interface Layer
   - [ ] Dto 생성 (Request + Response records)
   - [ ] ApiSpec interface 생성
   - [ ] Controller 생성

5. Test
   - [ ] 단위 테스트 (Entity, VO)
   - [ ] 통합 테스트 (Service)
   - [ ] E2E 테스트 (Controller)

6. Config
   - [ ] 캐시 설정 (필요 시)
   - [ ] 이벤트 리스너 (필요 시)
```
