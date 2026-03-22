package io.github.libxposed.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue

class XposedIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = emptyList()
}
