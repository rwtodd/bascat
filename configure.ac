#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.69])
AC_INIT([bascat], [1.1], [https://github.com/rwtodd/bascat])
AC_CONFIG_SRCDIR([main.c])
AC_CONFIG_HEADERS([config.h])

AM_INIT_AUTOMAKE([foreign subdir-objects])

# Checks for programs.
AC_PROG_CC
AC_PATH_PROG([DC_PATH],[dc])
AC_DEFINE_UNQUOTED([DC_PATH],["$ac_cv_path_DC_PATH"],[The path to the DC program, if found])

# Checks for libraries.

# Checks for header files.
AC_CHECK_HEADERS([fcntl.h stdint.h unistd.h stdbool.h],[],AC_MSG_ERROR([Can't continue: Missing header!]))

# Checks for typedefs, structures, and compiler characteristics.
AC_C_INLINE
AC_TYPE_INT16_T
AC_TYPE_SIZE_T
AC_TYPE_UINT16_T
AC_TYPE_UINT8_T

# Checks for library functions.
AC_FUNC_MMAP
if test "x$ac_cv_func_mmap_fixed_mapped" != xyes ; then
  AC_MSG_ERROR([You must have a working mmap() function!])
fi

AC_CONFIG_FILES([Makefile bascat.1])
AC_OUTPUT
