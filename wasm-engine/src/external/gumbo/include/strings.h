/*Dummy file to satisfy source file dependencies on Windows and WASM platform*/
#ifndef GUMBO_STRINGS_COMPAT_H
#define GUMBO_STRINGS_COMPAT_H

#ifdef _MSC_VER
/* Windows - map POSIX to Windows functions */
#define strcasecmp _stricmp
#define strncasecmp _strnicmp
#define inline __inline
#else
/* POSIX/WASM - functions are provided by system, define Windows aliases if needed */
#include_next <strings.h>
#define _stricmp strcasecmp
#define _strnicmp strncasecmp
#endif

#endif /* GUMBO_STRINGS_COMPAT_H */
