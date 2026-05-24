## What
<!-- What does this PR do? One sentence. -->

## Why
<!-- Why is this change needed? Link to issue/RFC. -->

## How
<!-- Technical approach. Key decisions. -->

## Testing
<!-- How was this tested? -->
- [ ] Unit tests pass (`cd backend && mvn test`)
- [ ] Architecture tests pass (`cd backend && mvn test -Dtest="*ArchitectureTest*"`)
- [ ] Integration tests pass (`cd backend && mvn verify -Pintegration-test`)
- [ ] Frontend lint passes (`cd frontend && npm run lint`)
- [ ] AI service lint passes (`cd ai-service && ruff check .`)
- [ ] Manual testing done
- [ ] Edge cases covered

## Screenshots
<!-- UI changes only -->

## Checklist
- [ ] Self-reviewed my code
- [ ] Added/updated tests
- [ ] Updated documentation (English + zh-Hans-CN + zh-Hant-TW if applicable)
- [ ] No new warnings
- [ ] Breaking changes documented
- [ ] Migration guide included (if breaking)
- [ ] Hexagonal architecture rules followed (no framework in domain/app)
