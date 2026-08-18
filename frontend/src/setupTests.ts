// Adds the DOM matchers (toBeInTheDocument and friends) to expect().
// Without this every component assertion fails to compile, which is how the one test
// file in this project came to be broken from the initial commit onwards.
import '@testing-library/jest-dom';
