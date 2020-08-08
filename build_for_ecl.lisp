;; a file I used to successfully build an .EXE from ECL for windows.
;; You will have to put ecl.dll in a place visible to the resulting exe.

(require 'asdf)
(require 'cmp)

(mapc #'(lambda (fl) (compile-file fl :system-p t)) 
   '("packages.lisp" "bascat.lisp"))

(c:build-program "bascat" :lisp-files '("packages.obj" "bascat.obj")
  :epilogue-code  '(progn (rt-bascat:bascat-main)(si:exit)))
;;   '(let ((args (cdr (ext:command-args))))
;;      (if (null args)
;;         (format t "usage: bascat <gwbas-file>~%")
;;         (rt-bascat:bascat (first args)))))
;;
