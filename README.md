# ecdtool

Tool for Working with CUE+FLAC CD Images

## Use Cases

  - Rename UTF-8 track names to ASCII file naming
  - Fix CD' pressing offset
  - Dump eCD into cue+wav, which can burned with cdrdao
    optionally balancing the cd burner's write offset

# How to create an eCD?

  - [EAC](http://exactaudiocopy.de/)
  - [dBpoweramp](https://www.dbpoweramp.com/cd-ripper.htm)

To create a cue sheet of an Audio CD, start EAC and select from the menu:
Action -> Create CUE Sheet -> Current Gap Settings

For the flac files use naming scheme: %tracknr2% %title%

Alternatively dump the flac files using dBpoweramp using the corresponding file
naming scheme.

## An sample eCD looks like:

```
Favourite Artist - 2015 - Debut Album:
├── 01 First Track.flac
├── 02 Second Track.flac
...
├── 12 Last Track.flac
├── folder.jpg
└── Debut Album.cue
```

## Credits

  - [open iconic](https://useiconic.com/open/)
