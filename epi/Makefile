#
# To run:
#    cd ..
#    for i in base ejws graphics math
#    do ln -s /usr/share/bzdev/libbzdev-$i.jar .
#    done
#    ln -s /usr/share/bzdev/libbzdev.jar .
#    ln -s /usr/share/bzdev/libosgbatik.jar .
#    java -jar epi.jar

all: ../epi.jar

../epi.jar: epi.mf *.java emodel/*.java model.html
	(cd ..; make libs)
	mkdir -p classes
	javac --release 11 -d classes \
		-classpath classes:/usr/share/bzdev/libbzdev.jar \
		*.java emodel/*.java
	jar cmf epi.mf ../epi.jar -C classes . model.html

clean:
	rm -rf classes

superclean:
	rm -rf classes
	rm ../epi.jar
