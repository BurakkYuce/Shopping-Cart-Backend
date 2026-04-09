declare module 'plotly.js-dist-min' {
  const Plotly: {
    newPlot(
      el: HTMLElement | string,
      data: unknown,
      layout?: unknown,
      config?: unknown,
    ): Promise<void>;
    react(
      el: HTMLElement | string,
      data: unknown,
      layout?: unknown,
      config?: unknown,
    ): Promise<void>;
    purge(el: HTMLElement | string): void;
    [key: string]: unknown;
  };
  export default Plotly;
  export const newPlot: typeof Plotly.newPlot;
  export const react: typeof Plotly.react;
  export const purge: typeof Plotly.purge;
}
