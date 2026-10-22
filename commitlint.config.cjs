module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'refactor', 'test', 'docs',
      'style', 'build', 'ci', 'chore', 'revert',
    ]],
    'scope-enum': [2, 'always', [
      'vehicle', 'web', 'server', 'prototype', 'sdk', 'repo', 'ci', 'deps',
    ]],
    'header-max-length': [2, 'always', 72],
    'subject-case': [0],
    'subject-full-stop': [2, 'never', '.'],
  },
};
