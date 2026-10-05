# QFit — Agent rules

## Hard rule: APBFit is read-only

**後續動作請視 APBFit 裡面的檔案為唯讀，所有變動都在 QFit 這邊。**

Agents working on QFit MUST treat every path under the APBFit repository (including `/agent/repos/APBFit`, `/agent/repos/apbfit`, and any checkout of Pixson-Lin/APBFit) as **read-only reference**.

| Allowed | Forbidden |
|---|---|
| Read APBFit source, docs, and history for reference | Edit, create, delete, move, or rename files in APBFit |
| Copy ideas / patterns into **QFit** | Commit, push, branch, or open PRs against APBFit |
| Quote APBFit snippets inside QFit docs | Run formatters, builds that write into APBFit, or “drive-by” fixes there |

If a change seems to belong in APBFit, document it in QFit and leave APBFit untouched unless the owner explicitly asks to modify APBFit in a separate task.

All implementation, docs, PoCs, and experiments for this project live in **QFit only**.
