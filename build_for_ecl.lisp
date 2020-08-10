;; a file I used to successfully build an .EXE from ECL for windows.
;; You will have to put ecl.dll in a place visible to the resulting exe.

(require 'asdf)
(require 'cmp)
(asdf:load-system "rt-bascat")  ;; seems necessary, even though it shouldn't be
(asdf:make-build "rt-bascat"
                 :type :program
                 :move-here #P"./"
		 :epilogue-code
		 '(progn (rt-bascat:bascat-main)(si:exit)))
(si:exit)
