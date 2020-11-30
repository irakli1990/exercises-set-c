"# exercises-set-c" 
    A tribe of N indigenous people dinners from a large pot that can hold up to M servings of a
    stew. When a person wants to eat, she helps herself from the pot, unless it is empty. If the pot
    gets empty, the person who took the last portion wakes up a cook and waits until the cook fills
    the pot. After refilling the pot, the cook returns to his nap.
    The following code shows a skeleton of a program that simulates the feasting. The class Pot
    contains methods for the cook and the feasting people. Synchronization between threads can be
    obtained using a pair of semaphores:
    • emptyPot semaphore is used to signalize that the pot is empty and should be refilled by the
    cook.
    • available semaphore has a positive value if the pot is not empty.
    The current number of servings is hold in servingsAvailable field.
    Complete the code of getServing() and fill() methods so that:
    • getServing() is called by a feasting person (instance of Person class) and allows to
    get a single portion from the pot. The number of available servings is updated using
    removeServing() method. If the pot becomes empty then the cook has to be woken up
    before the person can eat.
    • fill() is called by the cook and is responsible for refilling the pot (it may use insertServings()
    to update the number of servings).
    Both methods should use the semaphores to ensure correct execution of the simulation.