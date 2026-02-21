The webdemo has a couple of problems.

* I usually deploy it using docker. However there is no build automation for this (except for a docker file). Even worse: I usually deploy it on docker under Linux x86, but I am developing under macOS (Apple Silicon). So I'd want use an automated build pipeline that produces those. I prefer if I could host the build pipeline myself (under docker for Linux, maybe using act runners? 

* It uses an old version of the spring framework. This should be updated to a version that receives Security updates (for quite a long time in the future.) 

* It uses angularjs (the old Javascript based version), bootstrap and a patched version of codemirror (to support tinyscript). he implementation of the UI should be replaced with something modern. The UI should not change its look and feel (if possible). The implementation should be well structured and as simple as possible. Is React a good candidate?

