(defsystem "rt-bascat"
  :description "rt-bascat: display GW-BASIC tokenized files"
  :version "1.0"
  :author "Richard Todd <richard.wesley.todd@gmail.com>"
  :licence "MIT"
  :build-operation "program-op" ;; leave as is
  :build-pathname "bascat"
  :entry-point "rt-bascat:bascat-main"
;;  :depends-on ("optima.ppcre" "command-line-arguments")
  :components ((:file "packages")
               (:file "bascat" :depends-on ("packages"))))

