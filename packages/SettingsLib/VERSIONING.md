# a note on versioning

you might notice that the versioning this is a mess.

the reason why is that initially this started at 104.2.1 because it initially matched the jLib version, because it was initially only a dependency of jLib,
but, that makes no sense to anyone else, please see [this section in the jLib README](https://github.com/dot166/jOS_j-lib/blob/main/README.md#a-note-on-versioning) for more information on the mess that was the versioning

so, starting with SettingsLib 136, it will sort of use [semantic versioning](https://semver.org/),
its major version will be the AOSP SDK version that its sources match add 100 (because maven is doing its job, example, Android 16 - 36 - 136), the minor version will match AOSPs minor version,
and the patch version will be the build time in the following format ```yyyyMMddHHmm```.
