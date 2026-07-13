import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const styles = readFileSync(resolve(process.cwd(), 'src/index.css'), 'utf8');

describe('master-detail responsive CSS contract', () => {
  it('uses native container queries rather than viewport-only responsive utilities', () => {
    expect(styles).toContain('.master-detail-list-container');
    expect(styles).toContain('container-type: inline-size');
    expect(styles).toContain('@container (max-width: 639px)');
    expect(styles).toContain('.resume-list-grid');
    expect(styles).toContain('.job-filter-bar');
    expect(styles).toContain('.tracking-stats-grid');
  });
});
