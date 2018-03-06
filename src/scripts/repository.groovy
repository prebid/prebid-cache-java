@Grab('org.yaml:snakeyaml:1.17')

import org.yaml.snakeyaml.Yaml

println '########## GROOVY PRE-BUILD SCRIPT BEGIN ############'
print '1. Executing pre-build groovy script... '

String pwd = System.getProperty("user.dir")
def yml = new Yaml().load((pwd.concat('/src/main/resources/repository.yml') as File).text)
def activeProfile = yml['cache.profiles.active']
def classname = yml["cache.${activeProfile}.classname.canonical"]
def propertyConfiguration = yml["cache.${activeProfile}.property.configuration.classname"].uncapitalize()

println 'loading respository.yml.'
println "Found: profile: ${activeProfile} | classname: ${classname} | property: ${propertyConfiguration}"
println '2. Creating spring repository bean mapping from template... '

String template = new File(pwd.concat('/src/scripts/spring-repository-bean.template')).getText('UTF-8')
template = template.replace('${classname.canonical}', classname)
springXml = template.replace('${propertyConfiguration}', propertyConfiguration)
print template

def springXmlFile = new File(pwd.concat("/src/main/resources/spring-repository-bean.xml"))
springXmlFile.withWriter('UTF-8') { writer ->
    writer.write(springXml)
}

println '3. Done !!'
println '########## GROOVY PRE-BUILD SCRIPT END ############'

//example.stream(){println it.cache.redis.classname.canonical}