(defsystem "rt-bascat"
  :description "rt-bascat: display GW-BASIC tokenized files"
  :version "1.0"
  :author "Richard Todd <richard.wesley.todd@gmail.com>"
  :licence "GPL V3"
;;  :depends-on ("optima.ppcre" "command-line-arguments")
  :components ((:file "packages")
               (:file "bascat" :depends-on ("packages"))))

