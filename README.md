# iTunesExport

Converts iTunes Library.xml library export to m3u8 playlists for Subsonic / Airsonic.

Compile:
```
javac iTunesExport.java
```

Outputs: iTunesExport.class. Note that QueryStringManager.java is no longer used, but preserved for future use.

```
USAGE: iTunesExport <iTunes Library XML file> <output path> <new root path> [new root path base level]
```

Example:
```
java iTunesExport /path/to/iTunes\ Library.xml /path/to/playlists/ /new/root/path 5
```

Note that the ```new root path base level``` is the number of parent directories to remove from the source base path. For example, 2 would strip two parents: /path/to/my/music --> /my/music. The ```new root path``` will then be prepended to that: ```/new/root/path/my/music```
