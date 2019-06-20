# FapBot ![](https://img.shields.io/discord/425464794368442371.svg?style=flat-square)
#### &copy; Division Industries LLC

## Installation / Testing
FapBot can only be run for one discord server using one instance of the program. Therefore,
in order to test it, you must get your own bot key from [here](https://discordapp.com/developers), 
create a discord server, and invite the bot to ONLY that server. FapBot supports PaaS and has
been tested on both Heroku (where it is running for the official discord server) and Dokku (a
self-hosted Heroku alternative, more on that here: https://github.com/dokku/dokku). However, 
using a PaaS is not required, and you may run FapBot using the gradle run task.

## Contributing
All contributions must follow 
[Java's standard code conventions.](https://www.oracle.com/technetwork/java/codeconventions-150003.pdf) 
Failure to comply to these conventions will result in the denial of your contribution, regardless 
of the final functioning of the code.

### What does this mean?
Not a lot. Just DO **NOT** do stuff like this:

```java
public void example()
{
    System.out.println("bad code");
}
```

or this

```java
System.out.println( getName( bad_code ) );
```

or this

```java
public void AnotherBadExample() {}
```

The reason for this is that these are not standard Java practices, they are from other languages.
This makes reading your code difficult for others because this is not how they are used to seeing
Java. Speaking as someone who knows multiple programming languages, it is confusing to look at this 
because I immediately think this is one language when in reality, it is another.

* * *

Do **THIS** instead:

```java
public void example() {
    System.out.println("good code");
}
```

or this

```java
System.out.println(getName(goodCode));
```

or this

```java
public void anotherGoodExample() {}
```